(ns pallet.blobstore.s3
  "Amazon s3 provider for pallet"
  (:require
   [clojure.java.io :refer [input-stream]]
   [com.palletops.awaze.s3 :as s3]
   [com.palletops.aws.api :as aws]
   [pallet.blobstore :as blobstor]
   [pallet.blobstore.implementation :as implementation]
   [pallet.compute.jvm :as jvm]))

(defn buckets [api credentials]
  "Return a sequence of maps for containers"
  (aws/execute
     api
     (s3/list-buckets-map
      credentials
      {})))

(defn bucket-for-name
  "Filter buckets for the given bucket name."
  [buckets bucket-name]
  (first (filter #(= bucket-name (:name %)) buckets)))

(defn create-bucket
  "Create a bucket with the given bucket name."
  [api credentials bucket-name]
  (aws/execute
     api
     (s3/create-bucket-map
      credentials
      {:bucket-name bucket-name})))

(defn put-object-request [])
(defmulti put-object-request
  "Return a partial put-object request map for the payload"
  type)

(defmethod put-object-request :default
  [x]
  {:input-stream (input-stream x)})

(defmethod put-object-request java.io.File
  [file]
  {:file file})

(defmethod put-object-request String
  [s]
  {:input-stream (java.io.ByteArrayInputStream.
                  (.getBytes s java.nio.charset.StandardCharsets/UTF_8))})

(defn put-object
  "Put a payload into a s3 container at path."
  [api credentials bucket-name key payload]
  {:pre [payload]}
  (aws/execute
     api
     (s3/put-object-map
      credentials
      (merge {:bucket-name bucket-name
              :key key}
             (put-object-request payload)))))

(defn one-hour-from-now
  []
  (-> (doto (java.util.Calendar/getInstance)
        (.setTime (java.util.Date.))
        (.add java.util.Calendar/HOUR 1))
      .getTime))

(defn generate-presigned-url
  "Return a pre-signed url."
  [api credentials bucket-name key request-map]
  (aws/execute
     api
     (s3/generate-presigned-url-map
      credentials
      {:bucket-name bucket-name
       :key key
       :expiration (one-hour-from-now)
       :method (:method request-map :GET)
       :response-headers (dissoc request-map :method)})))


(deftype S3Service [credentials api]
  pallet.blobstore/Blobstore

  (sign-blob-request [blobstore container path request-map]
    (let [request (generate-presigned-url
                   api credentials container path request-map)]
      {:endpoint request}))

  (put-file [blobstore container path file]
    (when-not (bucket-for-name (buckets api credentials) container)
      (create-bucket api credentials container))
    (put-object api credentials container path file))

  (put [blobstore container path payload]
    (when-not (bucket-for-name (buckets api credentials) container)
      (create-bucket api credentials container))
    (put-object api credentials container path payload))

  (containers [blobstore]
    (map :name (buckets api credentials)))

  (close [blobstore]))

(defmethod implementation/service :pallet-s3
  [provider {:keys [identity credential endpoint api]
             :or {endpoint "US_EAST_1"}}]
  (let [credentials {:access-key identity :secret-key credential
                     :endpoint endpoint}
        api (or api (aws/start {}))]
    (S3Service.
     credentials
     api)))
