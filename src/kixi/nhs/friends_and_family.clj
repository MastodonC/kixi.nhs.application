(ns kixi.nhs.friends-and-family
  (:require [kixi.nhs.xls :as xls]
            [kixi.nhs.data.transform :as transform]))


(defn process-friends-and-family
  "Retrieves Friends & Family Test value
  for England, including Independent
  Sector Providers."
  [ckan-client recipe]
  (let [field (:field recipe)
        data  (first (xls/process-xls ckan-client recipe))]
    (when (seq data)
      (->> data
           (transform/filter-dataset recipe)
           (transform/enrich-dataset recipe)
           (map #(-> %
                     (dissoc :area-team)
                     (update-in [:value] str)))))))

(defn analysis
  "Receives a sequence of F&F recipes.
  Returns a sequences of all results from those recipes combined."
  [ckan-client recipes]
  (mapcat #(process-friends-and-family ckan-client %) recipes))
