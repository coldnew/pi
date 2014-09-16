(defproject pi "0.1.0-SNAPSHOT"
  :description "Hyper Local Information"
  :url "http://example.com/FIXME"
  :license {:name "MIT"
            :url ""}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2322"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [environ "1.0.0"]

                 ;; Web communications
                 [ring/ring-core "1.3.1"]
                 [ring/ring-defaults "0.1.1"]
                 [javax.servlet/servlet-api "2.5"] ;dev only ?
                 [http-kit "2.1.18"]
                 [com.taoensso/sente "1.1.0"]
                 [compojure "1.1.9"]

                 ;; Database
                 [org.clojure/java.jdbc "0.3.5"]
                 [postgresql/postgresql "8.4-702.jdbc4"]

                 ;; Client application
                 [om "0.7.3"]
                 [secretary "1.2.1"]
                 [sablono "0.2.22"]
                 [geo-clj "0.3.15"]
                 ]
  :min-lein-version "2.3.3"
  :main pi.main
  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-marginalia "0.8.0"]]
  :java-agents [[com.newrelic.agent.java/newrelic-agent "2.19.0"]]
  :cljsbuild {:builds
               [{:id "dev"
                 :source-paths ["src-cljs/pi/"]
                 :compiler {:output-to "resources/public/js/main.js"
                            :output-dir "resources/public/js/out"
                            :optimizations :none
                            :pretty-print true
                            :source-map true}}
                {:id "production"
                 :source-paths ["src-cljs/pi/"]
                 :compiler
                   {:output-to "resources/public/js/main.js"
                    ;:output-dir "resources/public/js/out"
                    :optimizations :advanced
                    :elide-asserts true
                    :pretty-print false
                    :output-wrapper false
                    ;:preamble ["react/react.min.js"]
                    ;:externs ["react/externs/react.js"]
                    :closure-warnings {:externs-validation :off
                                       :non-standard-jsdoc :off}}}]}
  )
