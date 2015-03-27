(ns kixi.nhs.application.main
  "Start up for application"
  (:gen-class)
  (:require [com.stuartsierra.component  :as component]
            [clojure.tools.logging       :as log]
            [kixi.nhs.application.system :as s]))

(def system)

(defn -main [& args]

  (org.slf4j.MDC/put "pipejine.q" "main")

  (let [new-system (s/new-system)]

    (log/info "Initialising system: " new-system)
    (alter-var-root #'system (constantly new-system))

    (log/info "Starting system: " system)
    (alter-var-root #'system component/start)))
