(ns kixi.nhs.incidence
  (:require [kixi.nhs.data.storage                         :as storage]
            [kixi.nhs.data.transform                       :as transform]
            [clj-time.format                               :as tf]
            [clj-time.core                                 :as t]
            [clojure.string                                :as str]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Indicator 65: Incidence of MRSA                                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-date [formatter d]
  (let [[start-str _] (str/split d #" to ")]
    (tf/parse formatter (str/trim start-str))))

(defn latest-month
  "Finds the latest month in data.
  It looks for a date using provided key.
  Returns a datetime."
  [k formatter data]
  (->> data
       (map #(parse-date formatter (k %)))
       t/latest))

(defn filter-latest
  "Filter dataset by latest period of coverage.
  We only check the first part of the period."
  [latest-str data]
  (filter #(.startsWith (:period_of_coverage %) latest-str) data))

(defn mrsa-incidence
  "Reads data from CKAN for a given resource_id,
  fitlers the latest value. Returns a sequence of maps."
  [ckan-client recipe-map]
  (let [data (storage/get-resource-data ckan-client (:resource-id recipe-map))]
    (when (seq data)
      (let [formatter      (tf/formatter "d/M/YYYY")
            timestamp      (latest-month :period_of_coverage formatter data)]
        (->> data
             (transform/filter-dataset recipe-map)
             (filter-latest (tf/unparse formatter timestamp))
             (transform/enrich-dataset recipe-map))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Indicator 66: Incidence of C difficile                                               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn enrich
  "Use latest month timestamp to enrich the map
  with year and period_of_coverage which are PKs in
  datastore.
  Stringifies the sum value."
  [timestamp formatter m]
  (-> m
      (assoc :date (str (t/year timestamp))
             :period_of_coverage (tf/unparse formatter timestamp))
      (update-in [:sum] str)
      (dissoc :cdi_count :reporting_period)))


(defn c-difficile-incidence
  "Calculates incidence of C difficile.
  Filters data for latest month, sums up values for all
  CCGs. Returns a sequence of maps."
  [ckan-client recipe-map]
  (let [data (storage/get-resource-data ckan-client (:resource-id recipe-map))]
    (when (seq data)
      (let [formatter      (tf/formatter "yyyy-MM-dd'T'HH:mm:ss")
            timestamp      (latest-month :reporting_period formatter data)
            updated-recipe (update-in recipe-map [:conditions] conj {:field :reporting_period
                                                                     :values #{(tf/unparse formatter timestamp)}})]
        (->> data
             (transform/filter-dataset updated-recipe)
             (transform/sum-sequence (:sum-field recipe-map) nil) ;; returns a single map since it's summing up all maps
             (enrich timestamp formatter) ;; this dataset has uncommon field names, updating it to match others
             (conj []) ;; dataset returned should be a sequence of maps
             (transform/enrich-dataset recipe-map))))))
