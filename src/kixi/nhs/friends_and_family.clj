(ns kixi.nhs.friends-and-family
  (:require [kixi.nhs.xls            :as xls]
            [kixi.nhs.data.transform :as transform]
            [clojure.tools.logging   :as log]))

(defn process-friends-and-family
  "Retrieves Friends & Family Test value
  for England, including Independent
  Sector Providers."
  [ckan-client recipe]
  (let [field (:field recipe)
        data  (xls/process-xls ckan-client recipe)]
    (when (seq data)
      (->> data
           (transform/filter-dataset recipe)
           (transform/enrich-dataset recipe)
           (map #(-> %
                     (dissoc :area-team)
                     (update-in [:value] str)))))))

(defn calculate-percentage-recommended
  "Calculates the percentage recommended for data prior Aug 2014."
  [data]
  (mapv #(let [{:keys [:extremely-likely :likely :total-responses]} %
               calculation (/ (+ extremely-likely likely)
                              total-responses)
               map-result {:percentage-recommended calculation}]
           (merge % map-result))
        data))

(defn process-friends-and-family-with-calculations
  "Retrieves Friends & Family Test value
  for England, including Independent
  Sector Providers. With calculations for
  months April to July."
  [ckan-client recipe]
  (log/infof "Processing recipe for indicator %s." (:indicator-id recipe))
  (let [field (:field recipe)
        data  (xls/process-xls ckan-client recipe)]
    (when (seq data)
      (->> data
           (transform/filter-dataset recipe)
           (calculate-percentage-recommended)
           (transform/enrich-dataset recipe)
           (map #(-> %
                     (dissoc :area-team :extremely-likely
                             :likely :total-responses)
                     (update-in [:value] str)))))))

(defn analysis
  "Receives a sequence of F&F recipes.
  Returns a sequences of all results from those recipes combined."
  [ckan-client recipes]
  (mapcat #(if (:calculation %)
             (process-friends-and-family-with-calculations ckan-client %)
             (process-friends-and-family ckan-client %)) recipes))
