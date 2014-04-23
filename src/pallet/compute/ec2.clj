(ns pallet.compute.ec2
  "Amazon ec2 provider for pallet"
  (:refer-clojure :exclude [proxy])
  (:require
   [clojure.core.async :as async :refer [<! alts!! chan timeout]]
   [clojure.string :as string]
   [taoensso.timbre :refer [debugf warnf infof tracef]]
   [com.palletops.awaze.ec2 :as ec2]
   [com.palletops.aws.api :as aws]
   [com.palletops.aws.instance-poller :as poller]
   [com.palletops.aws.keypair :refer [ensure-keypair]]
   [com.palletops.aws.security-group
    :refer [ensure-security-group open-security-group-port]]
   [pallet.compute :as compute]
   [pallet.compute.ec2.ami :as ami]
   [pallet.compute.ec2.protocols :as ec2-impl]
   [pallet.compute.ec2.static :as static]
   [pallet.compute.implementation :as implementation]
   [pallet.compute.protocols :as impl]
   [pallet.core.context :refer [with-domain]]
   [pallet.exception :refer [combine-exceptions]]
   [pallet.feature :refer [if-feature has-feature?]]
   [pallet.node :as node]
   [pallet.script :as script]
   [pallet.ssh.execute :refer [ssh-script-on-target]]
   [pallet.stevedore :as stevedore]
   [pallet.user :refer [UserUnconstrained]]
   [pallet.utils :refer [deep-merge map-seq maybe-assoc]]
   [pallet.utils.async :refer [from-chan go-try]]
   pallet.environment
   [schema.core :refer [validate]]))


;;; Meta
(defn supported-providers []
  ["pallet-ec2"])

;;; ## Nodes

;;; ### Tags
(def pallet-name-tag "pallet-name")
(def pallet-image-tag "pallet-image")
(def pallet-state-tag "pallet-state")

(defn base-name-tag
  "Return the group tag for a group"
  [node-name]
  {:key pallet-name-tag :value (name node-name)})

(defn image-tag
  "Return the image tag for a group"
  [group-spec]
  {:key pallet-image-tag
   :value (with-out-str
            (pr (-> group-spec
                    :image
                    (select-keys
                     [:image-id :os-family :os-version :os-64-bit
                      :login-user :packager]))))})

(defn name-tag
  "Return the group tag for a group"
  [node-name ip]
  {:key "Name"
   :value (str (name node-name)
               "_" (string/replace (or ip "noip") #"\." "-"))})

(defn state-tag
  "Return the state tag for a group"
  [node-state]
  {:key pallet-state-tag :value (with-out-str (pr node-state))})

(defn get-tag
  [info tag]
  (:value (first (filter #(= tag (:key %)) (:tags info)))))

(defn node-state-value
  "Return the value from the state-tag on a node's instance-info"
  [info]
  (when-let [state (get-tag info pallet-state-tag)]
    (read-string state)))

(defn node-image-value
  "Return the value from the image-tag on a node's instance-info"
  [info]
  (when-let [state (get-tag info pallet-image-tag)]
    (read-string state)))

(defn tag-instances [credentials api id-tags]
  "Tag instances. id-tags is a sequence of id, tag tuples.  A tag is a
  map with :key and :value keys."
  (debugf "tag-instances %s" (pr-str id-tags))
  (aws/execute
   api
   (ec2/create-tags-map
    credentials
    {:resources (map first id-tags)
     :tags (map second id-tags)})))

(def instance-ip (some-fn :public-ip-address :private-ip-address))

(defn instance-tags
  [node-name node-spec instance]
  [(base-name-tag node-name)
   (image-tag node-spec)
   (name-tag node-name (instance-ip instance))])

(defn id-tags [instance tags]
  (map #(vector (:instance-id instance) %) tags))

(defn tag-instance-state
  "Update the instance's state"
  [credentials info state]
  (let [old-state (get-tag info pallet-state-tag)]
    (tag-instances
     credentials
     [(:instance-id info)]
     (state-tag (merge old-state state)))))

(defn execute
  [service command args]
  (ec2-impl/execute service command args))

(defn ami-info
  [service ami-id]
  (ec2-impl/ami-info service ami-id))

(defn image-info [service ami image-atom]
  (if-let [i @image-atom]
    i
    (reset! image-atom
            (->
             (execute service ec2/describe-images-map {:image-ids [ami]})
             :images
             first))))

;;; ### Node
;; deftype Ec2Node [service info image]
(defn ssh-port [info] 22)
(defn primary-ip [info] (:public-ip-address info))
(defn private-ip [info] (:private-ip-address info))
(defn is-64bit? [info] (= "x86_64" (:architecture info)))


;; (group-name [node] (get-tag info pallet-group-tag))
(defn os-family [info] (:os-family (node-image-value info)))
(defn os-version [info] (:os-version (node-image-value info)))
(defn hostname [info] (:public-dns-name info))
(defn id [info] (:instance-id info))


(defn run-state [info]
  (let [state (-> info :state :name)]
    (case state
      "pending" :running
      "running" :running
      "shutting-down" :stopped
      "terminated" :terminated
      "stopping" :stopped
      "stopped" :stopped)))

;; (defn running? [node] (= "running" (-> info :state :name)))
;; (terminated? [node] (#{"terminated" "shutting-down"}
;;                        (-> info :state :name)))

(defn packager [info] (:packager (node-image-value info)))

(defn image-user [info]
  {:post [(validate UserUnconstrained %)]}
  {:username
   (:login-user (node-image-value info))
   })

(defn hardware [info]
    (let [id (keyword (:instance-type info))]
      (merge
       (select-keys (static/instance-types id) [:ram :cpus])
       {:id id
        :disks (:block-device-mappings info)
        :nics (:network-interfaces info)})))

(defn proxy [node] nil)

(defn node-map
  [info compute-service]
  (-> {:id (id info)
       :primary-ip (primary-ip info)
       :hostname (hostname info)
       :run-state (run-state info)
       :os-family (os-family info)
       :os-version (os-version info)
       :packager (packager info)
       :ssh-port 22
       :hardware (hardware info)
       :image-user (image-user info)
       :compute-service compute-service
       :provider-data info}
      (maybe-assoc :proxy (proxy info))))


;;; implementation detail names
(defn security-group-name
  "Return the security group name for a group"
  [node-name]
  (str "pallet-" (name node-name)))

(defn user-keypair-name
  [user]
  (str "pallet-" (:username user)))


;;; Compute service
;; (defn ensure-keypair [credentials api key-name user]
;;   (let [key-pairs (try (aws/execute
;;                         api (ec2/describe-key-pairs-map
;;                              credentials
;;                              {:key-names [key-name]}))
;;                        (catch com.amazonaws.AmazonServiceException _))]
;;     (debugf "ensure-keypair existing %s" key-pairs)
;;     (when (zero? (count key-pairs))
;;       (infof "Keypair '%s' not present. Creating it..." key-name)
;;       (aws/execute
;;        api
;;        (ec2/import-key-pair-map
;;         credentials
;;         {:key-name key-name
;;          :public-key-material (slurp (:public-key-path user))}))
;;       (infof "Keypair '%s' created." key-name ))))

;; (defn ensure-security-group [credentials api security-group-name]
;;   (let [sgs (try
;;               (aws/execute
;;                api
;;                (ec2/describe-security-groups-map
;;                 credentials {:group-names [security-group-name]}))
;;               (catch com.amazonaws.AmazonServiceException e))]
;;     (debugf "ensure-security-group existing %s" (pr-str sgs))
;;     (when-not (seq sgs)
;;       (infof "Security group '%s' not present. Creating it..." security-group-name)
;;       (aws/execute
;;        api
;;        (ec2/create-security-group-map
;;         credentials
;;         {:group-name security-group-name
;;          :description
;;          (str "Pallet created group for " security-group-name)}))
;;       (infof "Security group '%s' created. Opening SSH port..." security-group-name)
;;       (aws/execute
;;        api
;;        (ec2/authorize-security-group-ingress-map
;;         credentials
;;         {:group-name security-group-name
;;          :ip-permissions [{:ip-protocol "tcp"
;;                            :from-port 22
;;                            :to-port 22
;;                            :ip-ranges ["0.0.0.0/0"]}]}))
;;       (infof "SSH port is open for group '%s'" security-group-name))))

              ;; security-group (if (seq security-group)
              ;;                  security-group
              ;;                  (let [security-group (security-group-name
              ;;                                        node-name)]

              ;;                    security-group))

(defn- get-tags [credentials api node]
  (let [tags (aws/execute
              api
              (ec2/describe-tags-map
               credentials
               {:filters [{:name "resource-id" :values [(node/id node)]}]}))]
    (debugf "get-tags tags %s" tags)
    (into {} (map (juxt :key :value) (:tags tags)))))

(deftype Ec2NodeTag [credentials api]
  pallet.compute.protocols/NodeTagReader
  (node-tag [_ node tag-name]
    (debugf "node-tag %s %s" (node/id node) tag-name)
    (let [tags (get-tags credentials api node)]
      (debugf "node-tag tags %s" tags)
      (get tags tag-name)))
  (node-tag [_ node tag-name default-value]
    (debugf "node-tag %s %s %s"
            (node/id node) tag-name default-value)
    (let [tags (get-tags credentials api node)]
      (debugf "node-tag tags %s" tags)
      (get tags tag-name default-value)))
  (node-tags [_ node]
    (debugf "node-tags %s" (node/id node))
    (get-tags credentials api node))

  pallet.compute.protocols/NodeTagWriter
  (tag-node! [_ node tag-name value]
    (debugf "tag-node! %s %s %s" (node/id node) tag-name value)
    (aws/execute
     api
     (ec2/create-tags-map
      credentials
      {:resources [(node/id node)]
       :tags [{:key tag-name :value value}]})))
  (node-taggable? [_ node]
    (debugf "node-taggable? %s" (node/id node))
    true))

(defn- instances-response->instances
  [instances]
  (mapcat :instances (:reservations instances)))

(defn launch-options
  [node-count node-spec security-group key-name]
  (let [placement (-> {}
                      (maybe-assoc
                       :availability-zone
                       (-> node-spec :location :location-id))
                      (merge (-> node-spec :provider :pallet-ec2 :placement))
                      map-seq)]
    (deep-merge
     (select-keys (-> node-spec :node-spec :image) [:image-id])
     (dissoc (-> node-spec :provider :pallet-ec2) :placement)
     (->
      {:image-id (-> node-spec :image :image-id)
       :min-count node-count
       :max-count node-count
       :key-name key-name
       :security-groups [security-group]}
      (maybe-assoc :placement placement)
      (maybe-assoc :instance-type (-> node-spec :hardware :hardware-id))))))

(deftype Ec2Service
    [credentials api image-info environment instance-poller tag-provider]

  pallet.core.protocols/Closeable
  (close [_]
    (poller/stop instance-poller))


  pallet.compute.protocols.ComputeService
  (nodes [service ch]
    (with-domain :ec2
      (go-try ch
        (debugf "nodes")
        (let [instances (aws/execute
                         api (ec2/describe-instances-map credentials {}))]
          (>! ch {:targets (map
                            #(node-map % service)
                            (instances-response->instances instances))})))))

  pallet.compute.protocols/ComputeServiceNodeCreateDestroy

  (images [_ ch]
    (with-domain :ec2
      (go-try ch
        (>! ch {:images @image-info}))))


  (create-nodes [service node-spec user node-count
                 {:keys [node-name] :as options} ch]
    (when-not (every? (:image node-spec) [:os-family :os-version :login-user])
      (throw
       (ex-info
        "node-spec :image must contain :os-family :os-version :login-user keys"
        {:supplied (select-keys (:image node-spec)
                                [:os-family :os-version :login-user])})))
    (with-domain :ec2
      (go-try ch
        (debugf "create-nodes %s %s %s" node-count (:image node-spec) node-name)
        (let [[kp? key-name] (if-let [key-name (-> node-spec :image :key-name)]
                               [nil key-name]
                               [true (user-keypair-name user)])
              [sg? security-group] (if-let [security-group
                                            (-> node-spec :network
                                                :security-groups first)]
                                     [nil security-group]
                                     [true (security-group-name node-name)])

              c (chan)
              cs (chan)
              n (count (filter identity [kp? sg?]))]
          (when kp?
            (let [key-material (or (:public-key-path user)
                                   (slurp (:public-key-path user)))]
              (ensure-keypair api credentials key-name key-material c)))
          (when sg?
            (ensure-security-group
             api credentials security-group
             {:description (str "Pallet created group for " (name node-name))}
             cs)
            (let [{:keys [exception] :as r} (<! cs)]
              (if exception
                (>! c r)
                (open-security-group-port
                 api credentials security-group 22 c))))

          (if-let [e (combine-exceptions
                      (map :exception (from-chan (async/take n c))))]
            (>! ch {:exception e})
            (let [options (launch-options
                           node-count node-spec security-group key-name)]
              (debugf "run-nodes %s nodes" node-count)
              (infof "Creating %s node(s) with name '%s'..."
                     node-count (name node-name))
              (aws/submit api (ec2/run-instances-map credentials options) c)
              (let [resp (<! c)]
                (debugf "run-nodes run-instances options %s" options)
                (debugf "run-nodes run-instances %s" resp)
                (debugf "run-nodes %s"
                        (pr-str (-> resp :reservation :instances)))
                (if-let [instances (seq (-> resp :reservation :instances))]
                  (let [ids (map :instance-id instances)
                        notify-fn #(not= "pending" (-> % :state :name))
                        channel (chan)
                        idmaps (into {}
                                     (map
                                      #(vector % [{:channel channel
                                                   :notify-when-f notify-fn}])
                                      ids))]
                    ;; Wait for the nodes to come up
                    (poller/add-instances instance-poller idmaps)
                    (debugf "run-nodes Polling instances %s" idmaps)
                    (debugf "run-nodes Waiting for instances to come up")
                    (let [timeout (timeout (* 5 60 1000))
                          instances (->> (for [_ (range (count idmaps))]
                                           (first (alts!! [channel timeout])))
                                         (remove nil?))
                          running? (fn [node]
                                     (= "running" (-> node :state :name)))

                          good-instances (filter running? instances)]
                      (when-let [failed (seq
                                         (filter (complement running?)
                                                 instances))]
                        (warnf
                         "run-nodes Nodes failed to start %s"
                         (vec failed))
                        (warnf
                         "%s of %s node(s) failed to start for node-name '%s'"
                         (count failed) node-count (name node-name))
                        (aws/submit
                         api
                         (ec2/terminate-instances-map
                          credentials
                          {:instance-ids (mapv :instance-id failed)})))
                      (when (not= (count instances) (count idmaps))
                        (warnf "run-nodes Nodes still pending: %s"
                               (- (count idmaps) (count instances))))
                      (infof
                       "Created %s node(s) for '%s' (%s node(s) requested)"
                       (count good-instances) (name node-name) node-count)
                      (debugf "run-nodes Tagging")
                      (let [tags (map
                                  #(instance-tags node-name node-spec %)
                                  good-instances)
                            id-tags (mapcat id-tags good-instances tags)
                            good-instances (map
                                            #(update-in %1 [:tags] concat %2)
                                            good-instances tags)]
                        (aws/submit
                         api
                         (ec2/create-tags-map
                          credentials
                          {:resources (map first id-tags)
                           :tags (map second id-tags)})
                         c)
                        (>! ch
                            (merge
                             {:new-targets (map #(node-map % service)
                                                good-instances)}
                             (select-keys (<! c) [:exception]))))))
                  (>! ch resp)))))))))

  (destroy-nodes [_ nodes ch]
    (go-try ch
      (let [c (aws/submit
               api
               (ec2/terminate-instances-map
                credentials {:instance-ids (map node/id nodes)}))
            r (<! c)]
        (if (:exception r)
          (>! ch r)
          (>! ch {:old-targets nodes})))))


  pallet.compute.protocols/ComputeServiceNodeStop
  (stop-nodes
    [compute nodes ch]
    (with-domain :ec2
      (aws/submit
       api
       (ec2/stop-instances
        credentials {:instance-ids (map node/id nodes)})
       ch)))

  (restart-nodes
    [compute nodes ch]
    (with-domain :ec2
      (aws/submit
       api
       (ec2/start-instances
        credentials {:instance-ids (map node/id nodes)})
       ch)))

  ;; not implemented pallet.compute.protocols/ComputeServiceNodeSuspend


  pallet.compute.ec2.protocols.AwsParseAMI
  (ami-info [service ami-id]
    (-> (pallet.compute.ec2/image-info service ami-id image-info)
        ami/parse))

  pallet.environment.protocols.Environment
  (environment [_] environment)

  pallet.compute.protocols.NodeTagReader
  (node-tag [compute node tag-name]
    (pallet.compute.protocols/node-tag
     (.tag_provider compute) node tag-name))
  (node-tag [compute node tag-name default-value]
    (pallet.compute.protocols/node-tag
     (.tag_provider compute) node tag-name default-value))
  (node-tags [compute node]
    (pallet.compute.protocols/node-tags (.tag_provider compute) node))
  pallet.compute.protocols/NodeTagWriter
  (tag-node! [compute node tag-name value]
    (pallet.compute.protocols/tag-node! (.tag_provider compute) node tag-name value))
  (node-taggable? [compute node]
    (when (.tag_provider compute)
      (pallet.compute.protocols/node-taggable? (.tag_provider compute) node)))

  pallet.compute.protocols.ComputeServiceProperties
  (service-properties [compute]
    (assoc (bean compute) :provider :pallet-ec2))

  ec2-impl/AwsExecute
  (execute [compute command args]
    (aws/execute api (command credentials args))))


(defn regions
  "Return the ec2 regions for a service."
  [service]
  (->> (execute service ec2/describe-regions-map {})
       :regions
       (map :region-name)))

(defn availability-zones
  "Return the ec2 zones for the current region.  Returns a sequence of maps
  with :zone-name, :region-name and :messages keys."
  [service]
  (->> (execute service ec2/describe-availability-zones-map {})
       :availability-zones))

(defn region
  "Return the current region name."
  [service]
  (let [regions (->> (availability-zones service)
                     (map :region-name)
                     distinct)]
    (assert (= 1 (count regions)))
    (first regions)))

;; service factory implementation for ec2
(defmethod implementation/service :pallet-ec2
  [provider {:keys [identity credential region environment tag-provider
                    api poller]
             :or {region "US_EAST_1"}
             :as options}]
  (let [options (dissoc
                 options
                 :identity :credential :extensions :blobstore :environment)
        credentials {:access-key identity :secret-key credential :region region}
        api (or api (aws/start {}))
        poller (or poller (poller/start
                           {:credentials credentials
                            :api-channel (:channel api)}))
        tag-provider (or tag-provider (Ec2NodeTag. credentials api))]
    (Ec2Service.
     credentials
     api
     (atom {})
     environment
     poller
     tag-provider)))
