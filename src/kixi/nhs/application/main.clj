(ns kixi.nhs.application.main
  "Start up for application"
  (:gen-class)
  (:require [com.stuartsierra.component :as component]))

(def system)

(defn -main [& args]

  (org.slf4j.MDC/put "pipejine.q" "main")

  (alter-var-root #'system (fn [_] (component/start 'kixi.nhs.application/new-system))))
