(ns kixi.nhs.constitution
  (:require [kixi.nhs.data.transform :as transform]
            [kixi.nhs.data.storage   :as storage]
            [clojure.tools.logging   :as log]
            [kixi.nhs.xls            :as xls]))

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

(defn divide-fields [recipe data]
  (let [fields (:division-fields recipe)]
    (when (seq data)
      (let [region-data (per-region fields (:metadata recipe) data)]
        (->> [region-data]
             (transform/enrich-dataset recipe))))))


(defn sum-fields [recipe data]
  (when (seq data)
    (let [summed-up (transform/sum-sequence (:sum-field recipe) [(:sum-field recipe)] data)]
      (->> [summed-up]
           (map #(update-in % [:sum] str))
           (transform/enrich-dataset recipe)))))

(defmulti process-recipe (fn [ckan-client recipe] (:operation recipe)))

(defmethod process-recipe :division [ckan-client recipe]
  (log/infof "Processing recipe for indicator %s and operation %s" (:indicator-id recipe) (:operation recipe))
  (let [data (scrub (storage/get-resource-data ckan-client (:resource-id recipe)))]
    (divide-fields recipe data)))

(defmethod process-recipe :sum [ckan-client recipe]
  (log/infof "Processing recipe for indicator %s and operation %s" (:indicator-id recipe) (:operation recipe))
  (let [data (->> (xls/process-xls ckan-client recipe)
                  (transform/filter-dataset recipe))]
    (sum-fields recipe data)))

(defmethod process-recipe :none [ckan-client recipe]
  (log/infof "Processing recipe for indicator %s and operation %s" (:indicator-id recipe) (:operation recipe))
  (let [field (:field recipe)]
    (->> (xls/process-xls ckan-client recipe)
         (transform/filter-dataset recipe)
         (map #(-> %
                   (select-keys [field])
                   (update-in [field] str)))
         (transform/enrich-dataset recipe))))

(defn analysis [ckan-client recipes]
  (mapcat #(process-recipe ckan-client %) recipes))
