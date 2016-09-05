(defproject braid-onyx "0.1.0-SNAPSHOT"
  :description ""
  :url ""
  :license {:name ""
            :url ""}
  :dependencies [[aero "1.0.0-beta2"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.onyxplatform/onyx "0.9.10-beta1" :exclusions [prismatic/schema commons-codec commons-logging
                                                                    org.clojure/tools.reader]]
                 [org.onyxplatform/lib-onyx "0.9.7.1" :exclusions [commons-codec]]
                 [com.datomic/datomic-pro "0.9.5201" :exclusions [joda-time commons-codec
                                                                  org.apache.httpcomponents/httpcore
                                                                  org.apache.httpcomponents/httpclient]]
                 [org.onyxplatform/onyx-datomic "0.9.10.0-beta1" :exclusions [org.slf4j/slf4j-api commons-codec]]
                 [org.onyxplatform/onyx-elasticsearch "0.9.10.0-beta1" :exclusions [org.slf4j/slf4j-api]]
                 [org.postgresql/postgresql "9.3-1103-jdbc4"]
                 ]
  :source-paths ["src"]

  :main braid-onyx.core

  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :creds :gpg}}

  :profiles {:dev {:jvm-opts ["-XX:-OmitStackTraceInFastThrow"]
                   :global-vars {*assert* true}}
             :dependencies [[org.clojure/tools.namespace "0.2.11"]
                            [lein-project-version "0.1.0"]]
             :uberjar {:aot [lib-onyx.media-driver
                             braid-onyx.core]
                       :uberjar-name "peer.jar"
                       :global-vars {*assert* false}}})
