{:dev {:dependencies [[ch.qos.logback/logback-classic "1.1.1"]
                      [org.slf4j/jcl-over-slf4j "1.7.6"]]
       :checkout-deps-shares ^:replace [:source-paths :test-paths
                                        :compile-path]
       :plugins [[lein-pallet-release "RELEASE"]]}
 :no-checkouts {:checkout-shares ^:replace []} ; disable checkouts
 :clojure-1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
 :clojure-1.6 {:dependencies [[org.clojure/clojure "1.6.0-RC.1"]]}}
