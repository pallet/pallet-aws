(ns pallet.compute.ec2-test
  (:require
   [clojure.test :refer :all]
   [pallet.api :refer [group-spec node-spec]]
   [pallet.compute.ec2 :refer :all]))

(deftest launch-options-test
  (is (= {:security-group-ids ["sg"],
          :key-name "kn",
          :max-count 1,
          :min-count 1,
          :image-id "i"}
         (launch-options
          1
          (group-spec :gn :node-spec (node-spec :image {:image-id "i"}))
          "sg" "kn"))
      "minimal test")
  (is (= {:security-group-ids ["sg"],
          :key-name "kn",
          :max-count 1,
          :min-count 1,
          :image-id "i"
          :instance-type "m1.small"}
         (launch-options
          1
          (group-spec :gn
            :node-spec (node-spec :hardware {:hardware-id "m1.small"}
                                  :image {:image-id "i"}))
          "sg" "kn"))
      "hardware id test")
  (is (= {:security-group-ids ["sg"],
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
  (is (= {:security-group-ids ["sg"],
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
      "block-device-mapping")
  (is (= {:key-name "kn",
          :max-count 1,
          :min-count 1,
          :image-id "i"
          :security-group-ids ["sg"]
          :network-interfaces [{:device-index 0
                                :subnet-id "subnet-abcdef77"
                                :groups ["sg-abcdef88"]
                                :associate-public-ip-address true
                                :delete-on-termination true}]}
         (launch-options
          1
          (group-spec :gn
            :node-spec
            (node-spec
             :image {:image-id "i"}
             :provider {:pallet-ec2
                        {:network-interfaces
                         [{:device-index 0
                                :subnet-id "subnet-abcdef77"
                                :groups ["sg-abcdef88"]
                                :associate-public-ip-address true
                                :delete-on-termination true}]}}))
          "sg" "kn"))
      "network-interfaces"))

(deftest instance-tags-test
  (is (= [{:key "pallet-group", :value "abcd"}
          {:key "pallet-image", :value "{:image-id \"ami\"}"}
          {:key "Name", :value "abcd_1-2-3-4"}]
         (instance-tags {:group-name "abcd" :image {:image-id "ami"}}
                        {:public-ip-address "1.2.3.4"})) ))

(deftest ids-tags-test
  (is (= [["id" {:key "Name", :value "abcd_1-2-3-4"}]]
         (id-tags {:instance-id "id" :public-ip-address "1.2.3.4"}
                  [{:key "Name", :value "abcd_1-2-3-4"}])) ))
