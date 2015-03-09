(ns kixi.nhs.constitution
  (:require [kixi.nhs.data.transform :as transform]
            [kixi.nhs.data.storage   :as storage]))

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

(defn per-team-area
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

(defn per-region
  "Returns total for region (England),
  sums up data for all CCGs."
  [fields metadata data]
  (percentage-seen-within-x-days fields metadata "Region" "England" "England" data))

(defn scrub
  "Removes empty rows from the data,
  or the rows that do not contain
  required information."
  [data]
  (->> data
       (remove #(empty? (:area_team_code_1 %)))))

;; TODO lenses
;;  team-area-data (per-team-area fields (:metadata recipe) data)

(defn process-recipe [ckan-client recipe]
  (let [data           (scrub (storage/get-resource-data ckan-client (:resource-id recipe)))
        fields         (:division-fields recipe)]
    (when (seq data)
      (let [region-data    (per-region fields (:metadata recipe) data)]
        (->> [region-data]
             (transform/enrich-dataset recipe))))))

(defn analysis [ckan-client recipes]
  (mapcat #(process-recipe ckan-client %) recipes))
