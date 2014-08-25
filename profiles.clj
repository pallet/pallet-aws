{:dev {:dependencies [[ch.qos.logback/logback-classic "1.1.1"]
                      [org.slf4j/jcl-over-slf4j "1.7.6"]
                      [com.palletops/crates "0.1.2-SNAPSHOT"]]
       :checkout-deps-shares ^:replace [:source-paths :test-paths
                                        :compile-path]
       :plugins [[codox/codox.leiningen "0.6.4"]
                 [lein-marginalia "0.7.1"]
                 [lein-pallet-release "RELEASE"]
                 [com.palletops/lein-test-env "RELEASE"]]}
 :provided {:dependencies [[com.palletops/pallet "0.8.0-RC.9"]]}
 :doc {:dependencies [[com.palletops/pallet-codox "0.1.0"]]
       :codox {:writer codox-md.writer/write-docs
               :output-dir "doc/0.1/api"
               :src-dir-uri "https://github.com/pallet/pallet-aws/blob/develop"
               :src-linenum-anchor-prefix "L"}
       :aliases {"marg" ["marg" "-d" "doc/0.1/"]
                 "codox" ["doc"]
                 "doc" ["do" "codox," "marg"]}}
 :no-checkouts {:checkout-shares ^:replace []} ; disable checkouts
 :clojure-1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
 :clojure-1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
 :aws {:pallet/test-env {:test-specs [{:selector :amzn-linux-2013-092}]}
       ;; :dependencies [[com.palletops/pallet-aws "0.2.4-SNAPSHOT"]]
       }}
