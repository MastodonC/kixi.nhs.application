(ns kixi.nhs.incidence
  (:require [kixi.nhs.data.storage                         :as storage]
            [kixi.nhs.data.transform                       :as transform]
            [clj-time.format                               :as tf]
            [clj-time.core                                 :as t]
            [clojure.string                                :as str]
            [clojure.tools.logging                         :as log]
            [kixi.nhs.data.date                            :as date]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Indicator 65: Incidence of MRSA                                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mrsa-incidence
  "Reads data from CKAN for a given resource_id.
  Returns a sequence of maps."
  [ckan-client recipe-map]
  (log/infof "Processing recipe for indicator %s." (:indicator-id recipe-map))
  (let [data (storage/get-resource-data ckan-client (:resource-id recipe-map))]
    (when (seq data)
      (->> data
           (transform/filter-dataset recipe-map)
           (transform/enrich-dataset recipe-map)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Indicator 66: Incidence of C difficile                                               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn c-difficile-incidence
  "Calculates incidence of C difficile.
  Sums up values for all
  CCGs. Returns a sequence of maps."
  [ckan-client recipe-map]
  (log/infof "Processing recipe for indicator %s." (:indicator-id recipe-map))
  (let [data (storage/get-resource-data ckan-client (:resource-id recipe-map))]
    (when (seq data)
      (->> data
           (transform/filter-dataset recipe-map)
           (transform/split-by-key :reporting_period)
           (map #(let [m (transform/sum-sequence (:sum-field recipe-map) nil %)
                       t (-> (:reporting_period m) (date/str->date (tf/formatter "MMM-yy")))]
                   (-> m
                       (assoc :date (str (t/year t))
                              :period_of_coverage (:reporting_period m))
                       (update-in [:sum] str)
                       (dissoc :cdi_count :reporting_period))))
           (transform/enrich-dataset recipe-map)))))
