(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]))

(def system)

(defn init
  "Constructs the current development system."
  []
  (require '[kixi.nhs.application.system])

  (let [new-system (resolve 'kixi.nhs.application.system/new-system)]
    (alter-var-root #'system
                    (constantly (new-system)))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root #'system component/start))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop s)))))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start)
  nil)

(defn reset []
  (stop)
  (refresh :after 'user/go)
  nil)
