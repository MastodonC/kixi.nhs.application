(ns kixi.nhs.gp-survey
  (:require [kixi.nhs.xls :as xls]
            [kixi.nhs.data.transform :as transform]))

(defn sum
  "Add two cells.
  Returns a single numeric value."
  [data]
  (-> data
      first
      (select-keys [:overall_experience_of_making_an_appointment_very_good_percentage
                    :overall_experience_of_making_an_appointment_fairly_good_percentage])
      vals
      transform/add-when-not-empty))

(defn val->seq
  "Create a sequence containing
  map with a value, to be later
  processed as a resource."
  [v]
  [{:value (str v)}])

(defn access-to-gp-services
  "Retrieves GP Survey results and
  returns sum of columns:
  :overall_experience_of_making_an_appointment_very_poor
  :overall_experience_of_making_an_appointment_very_good_percentage."
  [ckan-client recipe]
  (let [field (:field recipe)]
    (->> (xls/process-xls ckan-client recipe)
         first ;; we just work on a single worksheet
         (transform/filter-dataset recipe)
         sum
         val->seq
         (transform/enrich-dataset recipe))))

(defn access-to-gp-services-recipes
  "Process all monthly resources for GP Survey
  data.
  Returns a sequence of maps, where each map is
  a indicator value from a single monthly recipe."
  [ckan-client recipes]
  (mapcat #(access-to-gp-services ckan-client %) recipes))

(defn access-to-nhs-dental-services
  "Retrieves GP Survey results and
  filters out an appropriate column."
  [ckan-client recipe]
  (let [field (:field recipe)]
    (->> (xls/process-xls ckan-client recipe)
         first ;; we just work on a single worksheet
         (transform/filter-dataset recipe)
         (map #(-> %
                   (clojure.set/rename-keys {field :value})
                   (update-in [:value] str)
                   (dissoc :ccg_name)))
         (transform/enrich-dataset recipe))))

(defn dental-services-recipes
  "Process all monthly resources for GP Survey
  data.
  Returns a sequence of maps, where each map is
  a indicator value from a single monthly recipe."
  [ckan-client recipes]
  (mapcat #(access-to-nhs-dental-services ckan-client %) recipes))
