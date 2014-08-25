(ns pallet.compute.ec2.support-test
  (:require
   [clojure.core.async :refer [<!! chan]]
   [clojure.test :refer :all]
   [com.palletops.aws.api :as api]
   [com.palletops.aws.vpc :refer :all]
   [pallet.compute :refer [nodes]]
   [pallet.crates.test-nodes :as test-nodes]
   [pallet.test-env
    :refer [*compute-service* *node-spec-meta* test-env]]
   [pallet.test-env.project :as project]))

(test-env test-nodes/node-specs project/project)

(deftest ^:support nodes-test
  (let [{:keys [credentials] :as props} (service-properties *compute-service*)]
    (is (nodes *compute-service*))))
