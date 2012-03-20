(ns pallet.compute.ec2-test
  (:require
   [clojure.test :refer :all]
   [pallet.api :refer [group-spec node-spec]]
   [pallet.compute.ec2 :refer :all]))

(deftest launch-options-test
  (is (= {:security-groups ["sg"],
          :key-name "kn",
          :max-count 1,
          :min-count 1,
          :image-id "i"}
         (launch-options
          1
          (group-spec :gn :node-spec (node-spec :image {:image-id "i"}))
          "sg" "kn"))
      "minimal test")
  (is (= {:security-groups ["sg"],
          :key-name "kn",
          :max-count 1,
          :min-count 1,
          :image-id "i"
          :instance-type "M1Small"}
         (launch-options
          1
          (group-spec :gn :node-spec (node-spec :hardware {:hardware-id "m1.small"}
                                                :image {:image-id "i"}))
          "sg" "kn"))
      "hardware id test")
  (is (= {:security-groups ["sg"],
          :key-name "kn",
          :max-count 1,
          :min-count 1,
          :image-id "i"
          :placement {:availability-zone "us-east-1d"}}
         (launch-options
          1
          (group-spec :gn
            :node-spec (node-spec
                        :image {:image-id "i"}
                        :location {:location-id "us-east-1d"}))
          "sg" "kn"))
      "availability zone")
  (is (= {:security-groups ["sg"],
          :key-name "kn",
          :max-count 1,
          :min-count 1,
          :image-id "i"
          :block-device-mapping [{:device-name "/dev/sdh"}]}
         (launch-options
          1
          (group-spec :gn
            :node-spec
            (node-spec
             :image {:image-id "i"}
             :provider {:pallet-ec2
                        {:block-device-mapping
                         [{:device-name "/dev/sdh"}]}}))
          "sg" "kn"))
      "block-device-mapping"))
