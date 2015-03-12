(ns kixi.nhs.constitution
  (:require [kixi.nhs.data.transform :as transform]
            [kixi.nhs.data.storage   :as storage]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers

(defn scrub
  "Removes empty rows from the data,
  or the rows that do not contain
  required information."
  [data]
  (->> data
       (remove #(empty? (:area_team_code_1 %)))))

(defn total
  "Filters specific field k from
  the data, parses numeric values
  and sums them up."
  [k data]
  (->> data
       (map k)
       (keep #(when-not (empty? %)
                (Integer/parseInt (clojure.string/replace % #"," ""))))
       (apply +)))

(defn percentage-seen-within-x-days
  "Calculates percentage seen within/after x days.
  Returns a map with the result, breakdown, level,
  year and period of coverage."
  [[k1 k2] metadata breakdown level level-description data]
  (merge metadata
         {:value (str (transform/divide (total k1 data)
                                        (total k2 data)))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Top level - Area Team

(defn area-team-level
  "Splits data by area team code and calculates
  the percentage of patients seen within/after x days.
  Returns a sequence of maps."
  [fields metadata data]
  (->> data
       (transform/split-by-key :area_team_code_1)
       (map #(percentage-seen-within-x-days fields
                                            metadata
                                            "Area Team Code"
                                            (:area_team_code_1 (first %))
                                            (:area_team (first %)) %))))

(defn update-resource
  "Generates data and updates existing
  resource in CKAN."
  [ckan-client recipe id data])

(defn create-new-resource
  "Creates new resource in CKAN, generates data
  and stores it in DataStore."
  [ckan-client recipe data]
  )

(defn per-area-team
  "Generates data for area team level."
  [ckan-client recipe]
  (let [fields  (:division-fields recipe)
        data    (scrub (storage/get-resource-data ckan-client (:raw-resource-id recipe)))]
    (area-team-level fields (:metadata recipe) data)))

(defn insert-per-area-team
  "Generates data for Area Team level and either
  creates a new resource or updates an existing one."
  [ckan-client recipe]
  (let [existing-resource (storage/get-resource-metadata ckan-client (:resource-name recipe))
        data              (per-area-team ckan-client recipe)]
    (if (seq existing-resource)
      (update-resource ckan-client recipe (:id existing-resource) data)
      (create-new-resource ckan-client recipe data))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Top level - Nation

(defn per-region
  "Returns total for region (England),
  sums up data for all CCGs."
  [fields metadata data]
  (percentage-seen-within-x-days fields metadata "Region" "England" "England" data))

(defn process-recipe [ckan-client recipe]
  (let [data           (scrub (storage/get-resource-data ckan-client (:resource-id recipe)))
        fields         (:division-fields recipe)]
    (when (seq data)
      (let [region-data    (per-region fields (:metadata recipe) data)]
        (->> [region-data]
             (transform/enrich-dataset recipe))))))

(defn analysis [ckan-client recipes]
  (mapcat #(process-recipe ckan-client %) recipes))
