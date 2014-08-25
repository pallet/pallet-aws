(ns pallet.compute.ec2
  "Amazon ec2 provider for pallet"
  (:require
   [clojure.core.async :refer [alts!! chan timeout]]
   [clojure.string :as string]
   [clojure.tools.logging :as logging :refer [debugf warnf infof]]
   [com.palletops.awaze.ec2 :as ec2]
   [com.palletops.aws.api :as aws]
   [com.palletops.aws.instance-poller :as poller]
   [pallet.action-plan :as action-plan]
   [pallet.compute :as compute]
   [pallet.compute.ec2.ami :as ami]
   [pallet.compute.ec2.protocols :as impl]
   [pallet.compute.ec2.static :as static]
   [pallet.compute.implementation :as implementation]
   [pallet.environment]
   [pallet.execute :as execute]
   [pallet.feature :refer [if-feature has-feature?]]
   [pallet.node :as node]
   [pallet.script :as script]
   [pallet.ssh.execute :refer [ssh-script-on-target]]
   [pallet.stevedore :as stevedore]
   [pallet.utils :refer [deep-merge map-seq maybe-assoc]]
   [schema.core :as schema :refer [maybe optional-key validate]]))


;;; Meta
(defn supported-providers []
  ["pallet-ec2"])

;;; ## Nodes

;;; ### Tags
(def pallet-group-tag "pallet-group")
(def pallet-image-tag "pallet-image")
(def pallet-state-tag "pallet-state")

(defn group-tag
  "Return the group tag for a group"
  [group-spec]
  {:key pallet-group-tag :value (name (:group-name group-spec))})

(defn image-tag
  "Return the image tag for a group"
  [group-spec]
  {:key pallet-image-tag
   :value (with-out-str
            (pr (-> group-spec
                    :image
                    (select-keys
                     [:image-id :os-family :os-version :os-64-bit
                      :login-user]))))})

(defn name-tag
  "Return the group tag for a group"
  [group-spec ip]
  {:key "Name"
   :value (str (name (:group-name group-spec))
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
  [group-spec instance]
  [(group-tag group-spec)
   (image-tag group-spec)
   (name-tag group-spec (instance-ip instance))])

(defn id-tags [instance tags]
  (map #(vector (:instance-id instance) %) tags))

(defn tag-instances-for-group-spec
  "Returns a sequence of instance infos with tags applied"
  [credentials api group-spec instances]
  (debugf "tag-instances-for-group-spec %s %s" group-spec (count instances))
  (let [tags (map #(instance-tags group-spec %) instances)
        id-tags (mapcat id-tags instances tags)]
    (tag-instances credentials api id-tags)
    (map #(update-in %1 [:tags] concat %2) instances tags)))

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
  (impl/execute service command args))

(defn ami-info
  [service ami-id]
  (impl/ami-info service ami-id))

(defn image-info [service ami image-atom]
  (if-let [i @image-atom]
    i
    (reset! image-atom
            (->
             (execute service ec2/describe-images-map {:image-ids [ami]})
             :images
             first))))

;;; ### Node
(deftype Ec2Node [service info image]
  pallet.node/Node
  (ssh-port [node] 22)
  (primary-ip [node] (:public-ip-address info))
  (private-ip [node] (:private-ip-address info))
  (is-64bit? [node] (= "x86_64" (:architecture info)))
  (group-name [node] (get-tag info pallet-group-tag))
  (os-family [node] (:os-family (node-image-value info)))
  (os-version [node] (:os-version (node-image-value info)))
  (hostname [node] (:public-dns-name info))
  (id [node] (:instance-id info))
  (running? [node] (= "running" (-> info :state :name)))
  (terminated? [node] (#{"terminated" "shutting-down"}
                       (-> info :state :name)))
  (compute-service [node] service)
  pallet.node/NodePackager
  (packager [node] nil)
  pallet.node/NodeImage
  (image-user [node] {:username (:login-user (node-image-value info))})
  pallet.node/NodeHardware
  (hardware [node]
    (let [id (keyword (:instance-type info))]
      (merge
       (select-keys (static/instance-types id) [:ram :cpus])
       {:id id
        :disks (:block-device-mappings info)
        :nics (:network-interfaces info)})))
  pallet.node/NodeProxy
  (proxy [node] nil))

(defn bootstrapped?
  "Predicate for testing if a node is bootstrapped."
  [node]
  (:bs (node-state-value (.info node))))


;;; implementation detail names
(defn security-group-name
  "Return the security group name for a group"
  [group-spec]
  (str "pallet-" (name (:group-name group-spec))))

(defn user-keypair-name
  [user]
  (str "pallet-" (:username user)))


;;; Compute service
(defn ensure-keypair [credentials api key-name user]
  (let [key-pairs (try (aws/execute
                        api (ec2/describe-key-pairs-map
                             credentials
                             {:key-names [key-name]}))
                       (catch com.amazonaws.AmazonServiceException _))]
    (debugf "ensure-keypair existing %s" key-pairs)
    (when (zero? (count key-pairs))
      (infof "Keypair '%s' not present. Creating it..." key-name)
      (aws/execute
       api
       (ec2/import-key-pair-map
        credentials
        {:key-name key-name
         :public-key-material (slurp (:public-key-path user))}))
      (infof "Keypair '%s' created." key-name ))))

(defn ensure-security-group [credentials api security-group-name]
  (let [sgs (try
              (aws/execute
               api
               (ec2/describe-security-groups-map
                credentials {:group-names [security-group-name]}))
              (catch com.amazonaws.AmazonServiceException e))]
    (debugf "ensure-security-group existing %s" (pr-str sgs))
    (when-not (seq sgs)
      (infof "Security group '%s' not present. Creating it..." security-group-name)
      (aws/execute
       api
       (ec2/create-security-group-map
        credentials
        {:group-name security-group-name
         :description
         (str "Pallet created group for " security-group-name)}))
      (infof "Security group '%s' created. Opening SSH port..." security-group-name)
      (aws/execute
       api
       (ec2/authorize-security-group-ingress-map
        credentials
        {:group-name security-group-name
         :ip-permissions [{:ip-protocol "tcp"
                           :from-port 22
                           :to-port 22
                           :ip-ranges ["0.0.0.0/0"]}]}))
      (infof "SSH port is open for group '%s'" security-group-name))))

(defn- get-tags [credentials api node]
  (let [tags (aws/execute
              api
              (ec2/describe-tags-map
               credentials
               {:filters [{:name "resource-id" :values [(node/id node)]}]}))]
    (debugf "get-tags tags %s" tags)
    (into {} (map (juxt :key :value) (:tags tags)))))

(deftype Ec2NodeTag [credentials api]
  pallet.compute.NodeTagReader
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

  pallet.compute.NodeTagWriter
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
  [node-count group-spec security-group key-name]
  (let [node-spec group-spec
        placement (-> {}
                      (maybe-assoc
                       :availability-zone
                       (-> node-spec :location :location-id))
                      (merge (-> node-spec :provider :pallet-ec2 :placement))
                      map-seq)]
    (deep-merge
     (select-keys (-> group-spec :node-spec :image) [:image-id])
     (dissoc (-> node-spec :provider :pallet-ec2) :placement)
     (->
      {:image-id (-> group-spec :image :image-id)
       :min-count node-count
       :max-count node-count
       :key-name key-name
       :security-groups [security-group]}
      (maybe-assoc :placement placement)
      (maybe-assoc :instance-type (-> node-spec :hardware :hardware-id))))))

(deftype Ec2Service
    [credentials api image-info environment instance-poller tag-provider
     jump-hosts]
  pallet.compute.ComputeService

  (nodes [service]
    (debugf "getting nodes")
    (letfn [(make-node [info] (Ec2Node. service info (atom nil)))]
      (let [instances (aws/execute
                       api (ec2/describe-instances-map credentials {}))]
        (map make-node (instances-response->instances instances)))))

  (ensure-os-family [_ {:keys [image group-name] :or {image {}} :as group-spec}]
    (when-not (:image-id image)
      (throw
       (ex-info
        (format "Group-spec %s :image does not specify an :image-id" group-name)
        {:group-spec group-spec
         :reason :no-image-id})))
    (if-let [missing-keys (seq (remove image [:os-family :os-version :user]))]
      (let [response (aws/execute
                      api
                      (ec2/describe-images-map
                       credentials {:image-ids [(:image-id image)]}))]
        (debugf "ensure-os-family images %s" response)
        (if-let [images (:images response)]
          (let [image (first images)]
            (let [image-details (ami/parse image)
                  group-spec (update-in
                              group-spec [:image]
                              (fn [image] (merge image-details image)))]
              (if (every? (:image group-spec) missing-keys)
                (do
                  (logging/warnf
                   (str
                    "group-spec %s :image does not specify the keys %s. "
                    "Inferred %s from the AMI %s with name \"%s\".")
                   group-name (vec missing-keys)
                   (zipmap missing-keys (map (:image group-spec) missing-keys))
                   (:image-id image) (:name image))
                  group-spec)
                (let [missing (vec (remove (:image group-spec) missing-keys))]
                  (throw
                   (ex-info
                    (format
                     (str "group-spec %s :image does not specify the keys %s. "
                          "Could not infer %s from the AMI %s with name %s.")
                     group-name (vec missing-keys) missing (:name image))
                    {:group-spec group-spec
                     :image-name (:name image)
                     :image-id (:image-id image)
                     :missing-keys missing}))))))
          (throw
           (ex-info
            (format "Image %s not found" (:image-id image))
            {:image-id (:image-id image)
             :reason :image-not-found}))))
      group-spec))

  (run-nodes [service group-spec node-count user init-script options]
    (when-not (every? (:image group-spec) [:os-family :os-version :login-user])
      (throw
       (ex-info
        "node-spec :image must contain :os-family :os-version :login-user keys"
        {:supplied (select-keys (:image group-spec)
                                [:os-family :os-version :login-user])})))
    (debugf "run-nodes %s %s %s"
            (:group-name group-spec) node-count (:image group-spec))
    (let [key-name (-> group-spec :image :key-name)
          key-name (if key-name
                     key-name
                     (let [key-name (user-keypair-name user)]
                       (ensure-keypair credentials api key-name user)
                       key-name))
          security-group (-> group-spec :node-spec :config :security-group)
          security-group (if (seq security-group)
                           security-group
                           (let [security-group (security-group-name
                                                 group-spec)]
                             (ensure-security-group
                              credentials api security-group)
                             security-group))]
      (debugf "run-nodes %s nodes" node-count)
      (infof "Creating %s node(s) in group '%s'..."
             node-count (name (:group-name group-spec)))
      (let [options (launch-options
                     node-count group-spec security-group key-name)
            resp (ec2/run-instances credentials options)]
        (debugf "run-nodes run-instances options %s" options)
        (debugf "run-nodes run-instances %s" resp)
        (debugf "run-nodes %s" (pr-str (-> resp :reservation :instances)))
        (letfn [(make-node [info] (Ec2Node. service info (atom nil)))]
          (when-let [instances (seq (-> resp :reservation :instances))]
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
              (let [timeout (timeout (* 5 60 1000))
                    instances (->> (for [_ (range (count idmaps))]
                                     (first (alts!! [channel timeout])))
                                   (remove nil?))
                    running? (fn [node] (= "running" (-> node :state :name)))
                    _ (debugf "run-nodes Waiting for instances to come up")
                    good-instances (filter running? instances)]
                (when-let [failed (seq
                                   (filter (complement running?) instances))]
                  (warnf "run-nodes Nodes failed to start %s" (vec failed))
                  (warnf
                   "%s of %s node(s) failed to start for group '%s'"
                   (count failed) node-count (name (:group-name group-spec)))
                  (aws/execute api
                               (ec2/terminate-instances-map
                                credentials
                                {:instance-ids (mapv :instance-id failed)})))
                (when (not= (count instances) (count idmaps))
                  (warnf "run-nodes Nodes still pending: %s"
                         (- (count idmaps) (count instances))))
                (infof "Created %s node(s) in group '%s' (%s node(s) requested)"
                       (count good-instances)
                       (name (:group-name group-spec))
                       node-count)
                (debugf "run-nodes Tagging")
                (->> good-instances
                     (tag-instances-for-group-spec credentials api group-spec)
                     (map make-node)))))))))

  (reboot [_ nodes])

  (boot-if-down [_ nodes])

  (shutdown-node [_ node user])

  (shutdown [self nodes user])

  (destroy-nodes-in-group [_ group-name]
    (let [nodes (aws/execute
                 api
                 (ec2/describe-instances-map
                  credentials
                  {:filters [{:name (str "tag:" pallet-group-tag)
                              :values [group-name]}]}))]
      (aws/execute
       api
       (ec2/terminate-instances-map
        credentials
        {:instance-ids
         (map :instance-id (instances-response->instances nodes))}))))

  (destroy-node [_ node]
    (aws/execute
     api
     (ec2/terminate-instances-map
      credentials {:instance-ids [(node/id node)]})))

  (images [_] @image-info)

  (close [_]
    (poller/stop instance-poller))

  pallet.compute.ec2.protocols.AwsParseAMI
  (ami-info [service ami-id]
    (-> (pallet.compute.ec2/image-info service ami-id image-info)
        ami/parse))

  pallet.environment.Environment
  (environment [_] environment)

  pallet.compute.NodeTagReader
  (node-tag [compute node tag-name]
    (compute/node-tag
     (.tag_provider compute) node tag-name))
  (node-tag [compute node tag-name default-value]
    (compute/node-tag
     (.tag_provider compute) node tag-name default-value))
  (node-tags [compute node]
    (compute/node-tags (.tag_provider compute) node))
  pallet.compute/NodeTagWriter
  (tag-node! [compute node tag-name value]
    (compute/tag-node! (.tag_provider compute) node tag-name value))
  (node-taggable? [compute node]
    (when (.tag_provider compute)
      (compute/node-taggable? (.tag_provider compute) node)))

  pallet.compute.ComputeServiceProperties
  (service-properties [compute]
    {:provider :pallet-ec2
     :identity (:access-key (.credentials compute))
     :credential (:secret-key (.credentials compute))
     :credentials (.credentials compute)
     :api (.api compute)
     :image-info @(.image_info compute)
     :environment (.environment compute)
     :instance-poller (.instance_poller compute)
     :tag-provider (.tag_provider compute)})

  impl/AwsExecute
  (execute [compute command args]
    (aws/execute api (command credentials args))))

(if-feature jump-hosts-protocol
            (do
              (require 'pallet.compute.protocols)
              (extend-protocol pallet.compute.protocols/JumpHosts
                Ec2Service
                (jump-hosts [service] (.jump_hosts service))))
            nil)

(def ServiceOptions
  {:identity String
   :credential String
   (optional-key :region) String
   (optional-key :environment) {schema/Keyword schema/Any}
   (optional-key :tag-provider) (maybe
                                 (schema/protocol pallet.compute/NodeTagReader))
   (optional-key :api) {schema/Keyword schema/Any}
   (optional-key :poller) {schema/Keyword schema/Any}})

;; service factory implementation for ec2
(defmethod implementation/service :pallet-ec2
  [provider {:keys [identity credential region environment tag-provider
                    api poller jump-hosts]
             :or {region "us-east-1"}
             :as options}]
  {:pre [(validate ServiceOptions options)]}
  (let [options (dissoc
                 options
                 :identity :credential :extensions :blobstore :environment)
        credentials {:access-key identity :secret-key credential
                     :region region}
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
     tag-provider
     jump-hosts)))
