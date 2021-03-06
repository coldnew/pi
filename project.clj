(defproject pi "0.1.0-SNAPSHOT"
  :description "Hyper Local Information"
  :url "http://example.com/FIXME"
  :license {:name "MIT"
            :url ""}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.stuartsierra/component "0.2.2"]
                 [org.clojure/core.cache "0.6.4"]
                 [org.clojure/core.memoize "0.5.6"]
                 [environ "1.0.0"]
                 [com.taoensso/encore "1.11.3"]
                 [org.clojure/data.json "0.2.5"]

                 ;; TODO [com.taoensso/tower "3.0.1"]
                 [crypto-password "0.1.3"]
                 [clj-time "0.8.0"]

                 [org.clojure/test.check "0.5.9"]

                 ;; Web communications
                 [ring/ring-core "1.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [javax.servlet/servlet-api "2.5"]
                 [http-kit "2.1.19"]
                 [com.taoensso/sente "1.2.0"]
                 [compojure "1.2.0"]

                 ;; Database
                 [com.datomic/datomic-free "0.9.4899"]

                 ;; Client application
                 [om "0.7.3"]
                 [secretary "1.2.1"]
                 ;; TODO [sablono "0.2.22"]
                 ;; TODO om-tools
                 [geo-clj "0.3.15"]
                 ]
  :min-lein-version "2.3.3"
  :test-paths ["test"]
  :main pi.main
  :plugins [[lein-datomic "0.2.0"]
            [lein-cljsbuild "1.0.3"]
            [lein-ancient "0.5.5"]
            [lein-marginalia "0.8.0"]]  
  :java-agents [[com.newrelic.agent.java/newrelic-agent "2.19.0"]]
  :profiles {:dev {:datomic {:config "resources/datomic/free-transactor-template.properties"
                             :db-uri "datomic:free://localhost:4334/pi"
                             :install-location "/"}
                   :env {:api-keys
                         {:google "AIzaSyAmmkhErSKLgONi0mEaNgxKtI25R5QIEeg"}}}
             :uberjar {:aot :all
                       :jar-name "pi.jar"
                       :uberjar-name "uberpi.jar"
                       :hooks [leiningen.cljsbuild]
                       :env {:api-keys
                             {:google "AIzaSyAmmkhErSKLgONi0mEaNgxKtI25R5QIEeg"}}}
             :phonegap {}}
  :cljsbuild {:builds
              {:dev {:source-paths ["src-cljs/pi/"]
                     :compiler {:output-to "resources/public/js/main.js"
                                :output-dir "resources/public/js/out"
                                :optimizations :none
                                :pretty-print true
                                :foreign-libs [{:file "resources/public/js/externs/moment.min.js"
                                                :provides ["pi.moment"]}]
                                :source-map true}}
               :uberjar {
                         :source-paths ["src-cljs/pi/"]
                         :resource-paths ["resources/public/"]
                         :jar true
                         :compiler
                         {:output-to "resources/public/js/main.js"
                          :optimizations :advanced
                          :elide-asserts true
                          :pretty-print false
                          :output-wrapper false
                          :preamble ["react/react.min.js"]
                          :externs ["react/externs/react.js"
                                    "resources/public/js/externs/moment.min.js"]
                          :foreign-libs [{:file "http://momentjs.com/downloads/moment.min.js"
                                          :provides ["pi.moment"]}]
                          :closure-warnings {:externs-validation :off
                                             :non-standard-jsdoc :off}}}
               :phonegap {
                          :source-paths ["src-cljs/pi"]
                          :resource-paths ["resources/public/"]
                          :compiler
                          {:output-to "resources/public/js/main.js"
                           :optimizations :advanced
                           :elide-asserts true
                           :pretty-print false
                           :output-wrapper false
                           :preamble ["react/react.min.js"]
                           :externs ["react/externs/react.js"
                                     "resources/public/js/externs/moment.min.js"]
                           :foreign-libs [{:file "http://momentjs.com/downloads/moment.min.js"
                                           :provides ["pi.moment"]}]
                           :closure-warnings {:externs-validation :off
                                              :non-standard-jsdoc :off}}}}}
              )
