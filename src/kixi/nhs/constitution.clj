(ns kixi.nhs.constitution
  (:require [kixi.nhs.data.transform :as transform]
            [kixi.nhs.data.storage   :as storage]
            [clojure.tools.logging   :as log]
            [kixi.nhs.xls            :as xls]
            [kixi.nhs.data.date      :as date]
            [clj-time.format         :as tf]
            [clj-time.core           :as t]
            [clojure.string          :as str]
            [clojure.edn             :as edn]))

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
  (if (contains? (first data) :area_team_code_1)
    (->> data
         (remove #(empty? (:area_team_code_1 %))))
    (->> data
         (remove #(empty? (:area_team_code %))))))

(defn divide-fields [recipe data]
  (let [fields (:division-fields recipe)]
    (when (seq data)
      (let [region-data (per-region fields (:metadata recipe) data)]
        (->> [region-data]
             (transform/enrich-dataset recipe))))))


(defn sum-fields [recipe data]
  (when (seq data)
    (->> data
         (transform/sum-sequence (:sum-field recipe) [(:sum-field recipe)])
         vector
         (map #(update-in % [:sum] str))
         (transform/enrich-dataset recipe))))

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

(defn null->nil
  "Replaces all values that are \"NULL\"
  with nil."
  [data]
  (clojure.walk/postwalk (fn [x] (if (= x "NULL") nil x)) data))

(defn sum-by-key
  "Sums up all values for a given key,
  removes all nil values for which there
  is not datapoints."
  [k data]
  (->> data
       (map #(transform/sum-sequence k nil %))
       (map #(-> %
                 (select-keys [:sum :year :period_name])
                 (clojure.set/rename-keys {:sum k})))
       (remove #(nil? (k %)))))

(defmethod process-recipe :sum-and-divide [ckan-client recipe]
  (log/infof "Processing recipe for indicator %s and operation %s" (:indicator-id recipe) (:operation recipe))
  (let [[k1 k2] (:division-fields recipe)
        data (->> (storage/get-resource-data ckan-client (:resource-id recipe))
                  (null->nil)
                  (transform/split-by-key :year))
        d1   (sum-by-key k1 data)
        d2   (sum-by-key k2 data)]
    (->> (concat d1 d2)
         (transform/split-by-key :year)
         (map #(apply conj %))
         (map #(hash-map :year (:year %)
                         :value (str (transform/divide (k1 %)
                                                       (k2 %)))))
         (transform/enrich-dataset recipe))))

(defn keyword->date [k]
  (-> k
      name
      (str/replace #"_" " ")
      (str/capitalize)
      (date/str->date (tf/formatter "MMM yy"))
      (date/date->str)))

(defn columns->resources
  [fields columns]
  (->> columns
       first
       (map (fn [[k v]]
              (when (contains? (set fields) k)
                {:period_of_coverage (keyword->date k)
                 :date (keyword->date k)
                 :value (str v)})))
       (remove nil?)))

(defmethod process-recipe :all-totals [ckan-client recipe]
  (log/infof "Processing recipe for indicator %s and operation %s" (:indicator-id recipe) (:operation recipe))
  (->> (xls/process-xls ckan-client recipe)
       (transform/filter-dataset recipe)
       (columns->resources (:fields recipe))
       (transform/enrich-dataset recipe)))

(defn period->period_of_coverage [year period]
  (-> (str year " " period)
      (date/str->date (tf/formatter "yyyy/yy MMMM"))
      (date/date->str)))

(defmethod process-recipe :all-periods [ckan-client recipe]
  (log/infof "Processing recipe for indicator %s and operation %s" (:indicator-id recipe) (:operation recipe))
  (->> (xls/process-xls ckan-client recipe)
       (map #(-> %
                 (select-keys (:fields-to-extract recipe))
                 (update-in [(:field recipe)] str)
                 (assoc :period (period->period_of_coverage (:year %) (:period %)))))
       (transform/enrich-dataset recipe)))


(defmulti quarter->period_of_coverage (fn [year quarter] quarter))

(defmethod quarter->period_of_coverage "1.0" [year quarter]
  (let [start (-> (str year " " "April 06")
                  (date/str->date (tf/formatter "yyyy/yy MMMM dd")))
        end   (-> start
                  (t/plus (t/months 3)))]
    (str (date/date->str start) " to " (date/date->str end))))

(defmethod quarter->period_of_coverage "2.0" [year quarter]
  (let [start (-> (str year " " "April 06")
                  (date/str->date (tf/formatter "yyyy/yy MMMM dd"))
                  (t/plus (t/months 3)))
        end   (-> start
                  (t/plus (t/months 3)))]
    (str (date/date->str start) " to " (date/date->str end))))

(defmethod quarter->period_of_coverage "3.0" [year quarter]
  (let [start (-> (str year " " "April 06")
                  (date/str->date (tf/formatter "yyyy/yy MMMM dd"))
                  (t/plus (t/months 6)))
        end   (-> start
                  (t/plus (t/months 3)))]
    (str (date/date->str start) " to " (date/date->str end))))

(defmethod quarter->period_of_coverage "4.0" [year quarter]
  (let [start (-> (str year " " "April 06")
                  (date/str->date (tf/formatter "yyyy/yy MMMM dd"))
                  (t/plus (t/months 9)))
        end   (-> start
                  (t/plus (t/months 3)))]
    (str (date/date->str start) " to " (date/date->str end))))

(defn process-quarters
  "Takes a sequence of 4 quarters and process them into
  a sequence of resource entries. Parses numerical quarter
  into a period of coverage."
  [recipe data]
  (let [year (first (keep #(:year %) data))]
    (map #(-> %
              (select-keys (:fields-to-extract recipe))
              (update-in [(:field recipe)] str)
              (assoc :quarter (quarter->period_of_coverage year (str (:quarter %)))
                     :year year))
         data)))

(defmethod process-recipe :all-quarters [ckan-client recipe]
  (log/infof "Processing recipe for indicator %s and operation %s" (:indicator-id recipe) (:operation recipe))
  (->> (xls/process-xls ckan-client recipe)
       (partition 4)
       (mapcat #(process-quarters recipe %))
       (transform/enrich-dataset recipe)))

(defn analysis-125 [ckan-client recipe]
  (->> (xls/process-xls ckan-client recipe)
       (transform/filter-dataset recipe)
       (transform/enrich-dataset recipe)
       (mapv #(update-in % [:value] str))))

(defn analysis [ckan-client]
  (let [recipes (-> (slurp "resources/recipes/constitution.edn")
                    edn/read-string)]
    (into [] (concat
              (mapcat #(process-recipe ckan-client %) (:constitution recipes))
              (analysis-125 ckan-client (:constitution-125 recipes))))))


