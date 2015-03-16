(ns kixi.nhs.constitution
  (:require [kixi.nhs.data.transform :as transform]
            [kixi.nhs.data.storage   :as storage]
            [cheshire.core           :as json]
            [clojure.tools.logging   :as log]))

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
  [[k1 k2] metadata lens-value lens-title data]
  (merge metadata
         {:lens_title lens-title
          :lens_value lens-value
          :value (str (transform/divide (total k1 data)
                                        (total k2 data)))}))

(defn area-team-level
  "Splits data by area team code and calculates
  the percentage of patients seen within/after x days.
  Returns a sequence of maps."
  [fields metadata data]
  (->> data
       (transform/split-by-key :area_team_code_1)
       (map #(percentage-seen-within-x-days fields
                                            metadata
                                            (:area_team_code_1 (first %))
                                            (:area_team (first %)) %))))

(defn find-resource [ckan-client alias]
  (storage/get-resource-metadata ckan-client alias))

(defn create-new-resource
  "Creates new resource in CKAN, generates data
  and stores it in DataStore. Uses alias as a unique identifier
  that is later used to query resources."
  [ckan-client recipe data]
  (let [dataset-id (:dataset-id recipe)]
    (log/infof "Creating new resource for indicator %s and lens %s" (:indicator-id recipe) (:lens recipe))
    (let [new-resource (json/encode {:package_id dataset-id
                                     :aliases (:alias recipe)
                                     :url "http://fix-me"})]
      (storage/create-new-resource ckan-client dataset-id new-resource))))

(defmulti produce-data (fn [ckan-client recipe data] (:lens recipe)))

(defmethod produce-data :nation [ckan-client recipe data]
  (log/infof "Producing nation level data for indicator: %s" (:indicator-id recipe))
  (percentage-seen-within-x-days (:division-fields recipe) (:metadata recipe) "Region" "England" data))

(defmethod produce-data :area-team [ckan-client recipe data]
  (log/infof "Producing area team level data for indicator: %s" (:indicator-id recipe))
  (let [data                 (area-team-level (:division-fields recipe) (:metadata recipe) data)
        existing-resource-id (:id (storage/get-resource-metadata ckan-client (:alias recipe)))]
    (if (seq existing-resource-id)
      (storage/update-existing-resource ckan-client existing-resource-id data)
      (create-new-resource ckan-client recipe data))))

(defn resource
  "Retrieves raw data and generates a resource based on the
  recipe.

  Depending on the lens that is specified in the recipe, it will
  either return a sequence of maps to be inserted as top-level
  board report data, or it will create/update a lower-level resource."
  [ckan-client recipe]
  (let [data (scrub (storage/get-resource-data ckan-client (:resource-id recipe)))]
    (when (seq data)
      (let [data (produce-data ckan-client recipe data)]
        (->> data
             (transform/enrich-dataset recipe))))))

(defn analysis [ckan-client recipes]
  (mapcat #(resource ckan-client %) recipes))
