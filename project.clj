(defproject kixi.nhs.application "0.1.0-SNAPSHOT"
  :description "NHS dashbaord data transformation and storage."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src"]
  :plugins [[lein-cljfmt "0.1.10"]
            [jonase/eastwood "0.2.1"]]

  :dependencies [[org.clojure/clojure        "1.6.0"]

                 [com.stuartsierra/component "0.2.2"]

                 [kixi/pipe                  "0.17.12"]
                 [kixi/ckan                  "0.1.3"]

                 ;; data
                 [cheshire                   "5.4.0"]
                 [incanter                   "1.5.6"]

                 ;; logging
                 [org.clojure/tools.logging  "0.3.0"]

                 [clj-time                   "0.9.0"]
                 [clj-excel "0.0.1"]]

  :min-lein-version "2.5.0"
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.8"]]}
             :uberjar {:main kixi.nhs.application.main
                       :aot [kixi.nhs.application.main]}})
