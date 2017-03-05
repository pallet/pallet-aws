(defproject org.domaindrivenarchitecture/pallet-aws "0.2.8.2"
  :description "A provider for using Pallet with AWS EC2, based on the AWS SDK."
  :url "http://palletops.com"
  :license {:name "All rights reserved"}
  :scm {:url "git@github.com:pallet/pallet-aws.git"}
  :dependencies [[org.domaindrivenarchitecture/pallet-aws-ops "0.2.3.1"
                  :exclusions [commons-logging]]
                 [com.palletops/pallet "0.8.12"]
                 [org.clojure/core.match "0.2.2"
                  :exclusions [org.clojure/clojure]]])
