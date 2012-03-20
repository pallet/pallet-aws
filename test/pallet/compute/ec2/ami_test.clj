(ns pallet.compute.ec2.ami-test
  (:require
   [clojure.test :refer :all]
   [pallet.compute.ec2.ami :refer [parse]]))

(deftest parse-test
  (is (= {:os-family :amzn-linux
          :os-version "2013.09.0"
          :user {:username "ec2-user"}}
         (parse {:owner-id "137112412989"
                 :name "amzn-ami-pv-2013.09.0.x86_64-ebs"
                 :description "Amazon Linux AMI x86_64 PV EBS"}))))

(deftest os-parser-test
  (testing "amzn linux"
    (is (= {:os-family :amzn-linux :os-version "0.9.7-beta"
            :user {:username "ec2-user"}}
           (parse {:owner-id "137112412989"
                   :description "Amazon Linux AMI.*"
                   :name "137112412989/amzn-ami-0.9.7-beta.i386-ebs"})))
    (is (= {:os-family :amzn-linux :os-version "0.9.7-beta"
            :user {:username "ec2-user"}}
           (parse {:owner-id "137112412989"
                   :description "Amazon Linux AMI.*"
                   :name "137112412989/amzn-ami-0.9.7-beta.x86_64-ebs"})))
    (is (= {:os-family :amzn-linux :os-version "0.9.7-beta"
            :user {:username "ec2-user"}}
           (parse
            {:owner-id "137112412989"
             :description "Amazon Linux AMI.*"
             :name "amzn-ami-us-east-1/amzn-ami-0.9.7-beta.x86_64.manifest.xml"})))
    (is (= {:os-family :amzn-linux :os-version "0.9.7-beta"
            :user {:username "ec2-user"}}
           (parse
            {:owner-id "137112412989"
             :description "Amazon Linux AMI.*"
             :name "amzn-ami-us-east-1/amzn-ami-0.9.7-beta.i386.manifest.xml"}))))
  (testing "amazon centos"
    (is (= {:os-family :centos :os-version "5.4"}
           (parse {:owner-id "137112412989"
                   :description "Amazon Centos"
                   :name "amazon/EC2 CentOS 5.4 HVM AMI"}))))
  (testing "canonical"
    (is
     (=
      {:os-family :ubuntu :os-version "12.04" :user {:username "ubuntu"}}
      (parse
       {:owner-id "099720109477"
        :name "ubuntu/images-milestone/ubuntu-precise-12.04-beta1-i386-server-20120229.1"}))))
  (testing "rightscale"
    (is (= {:os-family :centos :os-version "5.4"}
           (parse
            {:owner-id "411009282317"
             :name "rightscale-us-east/CentOS_5.4_x64_v4.4.10.manifest.xml"}))))
  (testing "rightimage"
    (is (= {:os-family :ubuntu :os-version "9.10"}
           (parse
            {:owner-id "411009282317"
             :name "411009282317/RightImage_Ubuntu_9.10_x64_v4.5.3_EBS_Alpha"})))
    (is (= {:os-family :windows :os-version "2008"}
           (parse {:owner-id "411009282317"
                   :name "411009282317/RightImage_Windows_2008_x64_v5.5.5"})))))
