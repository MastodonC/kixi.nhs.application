(ns kixi.nhs.board-report
  (:require [kixi.nhs.data.storage                         :as storage]
            [clojure.tools.logging                         :as log]
            [clojure.edn                                   :as edn]
            [cheshire.core                                 :as json]
            [kixi.ckan.data                                :as data]
            [kixi.nhs.data.transform                       :as transform]
            [kixi.nhs.patient-experience.deprivation       :as deprivation]
            [kixi.nhs.patient-experience.ethnicity         :as ethnicity]
            [kixi.nhs.patient-experience.gender-comparison :as gender]
            [clj-time.format                               :as tf]
            [clj-time.core                                 :as t]
            [clj-time.coerce                               :as tc]
            [kixi.nhs.constitution                         :as constitution]
            [kixi.nhs.friends-and-family                   :as ff]
            [kixi.nhs.gp-survey                            :as gp]
            [clojure.string                                :as str]
            [kixi.nhs.incidence                            :as incidence]
            [kixi.nhs.data.schema                           :as schema]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Idicator 57: Bereaved carers' views on the quality of care                           ;;
;;              in the last 3 months of life                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn end-of-life-care
  "Reads data from CKAN for a given resource_id,
  filters on conditions, sums up three indicator
  values with reference to Outstanding, Excellent and Good
  (that are already filtered) and returns a sequence of
  results for each period."
  [ckan-client recipe-map]
  (->> (storage/get-resource-data ckan-client (:resource-id recipe-map))
       (transform/filter-dataset recipe-map)
       (transform/split-by-key :period_of_coverage)
       (map #(transform/sum-sequence :indicator_value [:question_response :indicator_value] %))
       (map #(update-in % [:sum] str))
       (transform/enrich-dataset recipe-map)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Simple datasets                                                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn read-dataset
  "Reads data from CKAN for a given resource-id,
  filters on conditions and outputs a vector of
  maps where each map is enriched with indicator-id
  and metadata from the recipe."
  [ckan-client recipe-map resource_id]
  (->> (storage/get-resource-data ckan-client resource_id)
       (transform/filter-dataset recipe-map)
       (transform/enrich-dataset recipe-map)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Process all recipes and update board report resource                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn read-config
  "Reads the config file and returns it as a string."
  [url]
  (-> (slurp url) edn/read-string))

(defn create-boardreport-dataset
  "Creates a sequence of maps containing the info
  needed for the board report."
  [ckan-client config]
  (concat (ethnicity/analysis ckan-client (:ethnicity config))
          (deprivation/analysis ckan-client (:deprivation config))
          (gender/analysis ckan-client (:gender config))
          (end-of-life-care ckan-client (:end-of-life-care config))
          (incidence/mrsa-incidence ckan-client (:mrsa-incidence config))
          (incidence/c-difficile-incidence ckan-client (:c-difficile-incidence config))
          (mapcat (fn [dataset-config]
                    (read-dataset ckan-client dataset-config
                                  (:resource-id dataset-config)))
                  (:simple-datasets config))
          (constitution/analysis ckan-client (:constitution config))
          (ff/analysis ckan-client (:friends-and-family config))
          (gp/access-to-gp-services-recipes ckan-client (:access-to-gp-services config))
          (gp/dental-services-recipes ckan-client (:access-to-nhs-dental-services config))))

(defn insert-new-dataset-and-resource
  "Creates new dataset, new resource and populates it with
  board report data."
  [ckan-client config-url]
  (let [config          (read-config config-url)
        new-dataset     (json/encode (:board-report-details config))
        new-dataset-id  (storage/create-new-dataset ckan-client new-dataset)
        new-resource    (json/encode {:package_id new-dataset-id
                                      :url "http://fix-me" ;; url is mandatory
                                      :description "Board report resource"})
        new-resource-id (storage/create-new-resource ckan-client new-dataset-id new-resource)
        records         (create-boardreport-dataset ckan-client config)
        data            (data/prepare-resource-for-insert new-dataset-id new-resource-id
                                                          {"records"     records
                                                           "fields"      (:fields schema/board-report-schema)
                                                           "primary_key" (:primary-key schema/board-report-schema)})]
    (storage/insert-new-resource ckan-client new-dataset-id data)))

(defn insert-board-report-resource
  "Uses existing dataset and adds a new resource
  to it. Populates it with board report data."
  [ckan-client config-url dataset-id]
  (let [config          (read-config config-url)
        new-resource    (json/encode {:package_id dataset-id
                                      :url "http://fix-me" ;; url is mandatory
                                      :description "Data for the board report"})
        new-resource-id (storage/create-new-resource ckan-client dataset-id new-resource)
        records         (create-boardreport-dataset ckan-client config)
        data            (data/prepare-resource-for-insert dataset-id new-resource-id
                                                          {"records"     records
                                                           "fields"      (:fields schema/board-report-schema)
                                                           "primary_key" (:primary-key schema/board-report-schema)})]
    (storage/insert-new-resource ckan-client dataset-id data)))


(defn update-board-report-dataset
  "Update existing rows in the table and append any new ones. Primary key is:
  (indicator_id, year, period_of_coverage)"
  [ckan-client resource-id config-url]
  (let [config          (read-config config-url)
        records         (create-boardreport-dataset ckan-client config)
        data            (json/encode {"records" records
                                      "method" "upsert"
                                      "force" true
                                      "resource_id" resource-id})]

    (storage/update-existing-resource ckan-client resource-id data)))

;; To insert new board report resource:
;; (insert-new-dataset-and-resource (:ckan-client system) "resources/prod_config.edn")
;; To insert new resource into existing dataset:
;; (insert-board-report-resource (:ckan-client system) "resources/prod_config.edn" "board_report")
;; To update existing board resource (preferable):
;; (update-board-report-dataset (:ckan-client system) "1a771670-c761-49a7-b34c-1d61b21602dc" "resources/prod_config.edn")
