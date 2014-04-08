{:dev {:dependencies [[ch.qos.logback/logback-classic "1.1.1"]
                      [org.slf4j/jcl-over-slf4j "1.7.6"]]
       :checkout-deps-shares ^:replace [:source-paths :test-paths
                                        :compile-path]
       :plugins [[codox/codox.leiningen "0.6.4"]
                 [lein-marginalia "0.7.1"]
                 [lein-pallet-release "0.1.3"]]
       :pallet-release
       {:url "https://pbors:${GH_TOKEN}@github.com/pallet/pallet-aws.git",
        :branch "master"}
       :injections [(require 'pallet.log)
                    (pallet.log/default-log-config)]}
 :doc {:dependencies [[com.palletops/pallet-codox "0.1.0"]]
       :codox {:writer codox-md.writer/write-docs
               :output-dir "doc/0.1/api"
               :src-dir-uri "https://github.com/pallet/pallet-aws/blob/develop"
               :src-linenum-anchor-prefix "L"}
       :aliases {"marg" ["marg" "-d" "doc/0.1/"]
                 "codox" ["doc"]
                 "doc" ["do" "codox," "marg"]}}
 :release
 {:set-version
  {:updates [{:path "README.md" :no-snapshot true}]}}
 :no-checkouts {:checkout-shares ^:replace []} ; disable checkouts
 :clojure-1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
 :clojure-1.6 {:dependencies [[org.clojure/clojure "1.6.0-RC.1"]]}}
