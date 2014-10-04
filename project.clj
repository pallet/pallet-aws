(defproject com.palletops/pallet-aws "0.2.5-SNAPSHOT"
  :description "A provider for using Pallet with AWS EC2, based on the AWS SDK."
  :url "http://palletops.com"
  :license {:name "All rights reserved"}
  :scm {:url "git@github.com:pallet/pallet-aws.git"}
  :dependencies [[com.palletops/pallet-aws-ops "0.2.1"
                  :exclusions [commons-logging]]
                 [com.palletops/pallet "0.8.0-RC.9"]
                 [org.clojure/core.match "0.2.0"
                  :exclusions [org.clojure/clojure]]])
