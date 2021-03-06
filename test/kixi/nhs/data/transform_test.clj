(ns kixi.nhs.data.transform-test
  (:use clojure.test)
  (:require [kixi.nhs.data.transform :as transform]))

(deftest parse-number
  (is (= 21
         (transform/parse-number "21")))
  (is (= 0.01
         (transform/parse-number "0.01")))
  (is (= 2000
         (transform/parse-number "2,000")))
  (is (= 2000.1
         (transform/parse-number "2,000.1"))))

(deftest split-by-field-value-test
  (is (= {2009 [{:v 0, :year 2009}], 2011 [{:v 3, :year 2011} {:v 7, :year 2011}], 2010 [{:v 5, :year 2010} {:v 4, :year 2010}]}
         (transform/split-by-field-value :year
                                         [{:year 2010 :v 5} {:year 2010 :v 4} {:year 2011 :v 3}
                                          {:year 2011 :v 7} {:year 2009 :v 0}]))))

(deftest outer-join-test
  (is (= [{:common-field "A" "01" 1 "02" 3} {:common-field "B" "01" 2 "02" 4} {:common-field "C" "02" 1}]
         (transform/outer-join :k
                               (fn [k data] (let [d (get data k)]
                                              (when (seq d)
                                                (hash-map :common-field (:k d)
                                                          (:id d) (:v d)))))
                               [[{:v 1 :id "01" :k "A"} {:v 2 :id "01" :k "B"}]
                                [{:v 3 :id "02" :k "A"} {:v 4 :id "02" :k "B"} {:v 1 :id "02" :k "C"}]]))))

(deftest remove-ids-test
  (is (= [{"Level" "National", "DSR" "70", "Breakdown" "National", "Gender" "Person", "CI Upper" "70.7", "Level Description" "All registered patients in England", "Observed" "36,265", "Year" "2009", "CI Lower" "69.2", "Registered patients" "51,450,031"}
          {"Level" "National", "DSR" "45.2", "Breakdown" "National", "Gender" "Female", "CI Upper" "46", "Level Description" "All registered patients in England", "Observed" "11,502", "Year" "2009", "CI Lower" "44.3", "Registered patients" "25,450,302"}]
         (transform/remove-ids [{"Level" "National", "DSR" "70", "Breakdown" "National", "Gender" "Person", "CI Upper" "70.7", "Level Description" "All registered patients in England", "Observed" "36,265", "_id" 13, "Year" "2009", "CI Lower" "69.2", "Registered patients" "51,450,031"} {"Level" "National", "DSR" "45.2", "Breakdown" "National", "Gender" "Female", "CI Upper" "46", "Level Description" "All registered patients in England", "Observed" "11,502", "_id" 14, "Year" "2009", "CI Lower" "44.3", "Registered patients" "25,450,302"}]))))


(deftest filter-dataset-test
  (testing "Testing filtering dataset."
    ;; Checks return a sequence of maps, filters on indicator field
    ;; and conditions, keeps indicator value & year:
    (is (= [{:indicator_value "85.7", :year "2013/14" :period_of_coverage "July 2013 to March 2014"}
            {:indicator_value "86.7", :year "2012/13" :period_of_coverage "July 2012 to March 2013"}]
           (transform/filter-dataset
            {:indicator-id "22"
             :fields-to-extract [:indicator_value :year :period_of_coverage]
             :conditions [{:field :level :values #{"England"}}]
             :resource-id "2b10e7b4-c799-44f9-81d6-3a42f4260893"}
            [{:level "England", :denominator "883852.0", :breakdown "England",
              :question_response_rate "97.9", :_id 1, :indicator_id nil,
              :year "2013/14", :level_description "England", :indicator_value "85.7",
              :period_of_coverage "July 2013 to March 2014", :numerator "757456.1"}
             {:level "England", :denominator "948507.5", :breakdown "England",
              :question_response_rate "97.7", :_id 2, :indicator_id nil, :year "2012/13",
              :level_description "England", :indicator_value "86.7", :period_of_coverage
              "July 2012 to March 2013", :numerator "822728.3"}
             {:level "Wales", :denominator "1009576.2", :breakdown "England",
              :question_response_rate "97.3", :_id 3, :indicator_id nil, :year "2011/12",
              :level_description "England", :indicator_value "88.3",
              :period_of_coverage "July 2011 to March 2012", :numerator "891213.9"}])))
    ;; Checks the sequence returned is empty when indicator field not found:
    (is (empty? (transform/filter-dataset
                 {:indicator-id "22"
                  :fields-to-extract [:value :year :period_of_coverage]
                  :conditions [{:field :level :values #{"England"}}]
                  :resource-id "2b10e7b4-c799-44f9-81d6-3a42f4260893"}
                 [{:level "Wales", :denominator "1009576.2", :breakdown "England",
                   :question_response_rate "97.3", :_id 3, :indicator_id nil,
                   :year "2011/12", :level_description "England", :indicator_value "88.3",
                   :period_of_coverage "July 2011 to March 2012", :numerator "891213.9"}])))
    ;; Checks the sequence returned is empty when conditions not met:
    (is (empty (transform/filter-dataset
                {:indicator-id "22"
                 :fields-to-extract [:indicator_value :year :period_of_coverage]
                 :conditions [{:field :level :values #{"Scotland"}}]
                 :resource-id "2b10e7b4-c799-44f9-81d6-3a42f4260893"}
                [{:level "England", :denominator "883852.0", :breakdown "England",
                  :question_response_rate "97.9", :_id 1, :indicator_id nil,
                  :year "2013/14", :level_description "England", :indicator_value "85.7",
                  :period_of_coverage "July 2013 to March 2014", :numerator "757456.1"}
                 {:level "England", :denominator "948507.5", :breakdown "England",
                  :question_response_rate "97.7", :_id 2, :indicator_id nil,
                  :year "2012/13", :level_description "England", :indicator_value "86.7",
                  :period_of_coverage "July 2012 to March 2013", :numerator "822728.3"}
                 {:level "Wales", :denominator "1009576.2", :breakdown "England",
                  :question_response_rate "97.3", :_id 3, :indicator_id nil,
                  :year "2011/12", :level_description "England", :indicator_value "88.3",
                  :period_of_coverage "July 2011 to March 2012", :numerator "891213.9"}])))
    ;; Checks the sequence returned is empty when no dataset:
    (is (empty?
         (transform/filter-dataset {:indicator-id "22"
                                    :fields-to-extract [:indicator_value :year :period_of_coverage]
                                    :conditions [{:field :level :values #{"England"}}]
                                    :resource-id "2b10e7b4-c799-44f9-81d6-3a42f4260893"}
                                   [])))))

(deftest enrich-dataset-test
  (testing "Testing enriching dataset."
    ;; Checks the :indicator_value keyword is renamed to :value,
    ;; the "Indicator id" is added, and the result is a vector:
    (is (= [{:indicator_id "22" :value "85.7" :date "2013/14"
             :period_of_coverage "July 2013 to March 2014"
             :start_date "2013-07-01" :end_date "2014-03-31"}
            {:indicator_id "22" :value "86.7" :date "2012/13"
             :period_of_coverage "July 2012 to March 2013"
             :start_date "2012-07-01" :end_date "2013-03-31"}
            {:indicator_id "22" :value "88.3" :date "2011/12"
             :period_of_coverage "July 2011 to March 2012"
             :start_date "2011-07-01" :end_date "2012-03-31"}]
           (transform/enrich-dataset
            {:indicator-id "22"
             :fields-to-rename {:year :date :indicator_value :value}
             :conditions [{:field :level :values #{"Scotland"}}]
             :format {:percentage :none}
             :resource-id "2b10e7b4-c799-44f9-81d6-3a42f4260893"}
            [{:year "2013/14" :indicator_value "85.7" 
              :period_of_coverage "July 2013 to March 2014"}
             {:year "2012/13" :indicator_value "86.7"
              :period_of_coverage "July 2012 to March 2013"}
             {:year "2011/12" :indicator_value "88.3" 
              :period_of_coverage "July 2011 to March 2012"}])))
    (is (= [{:indicator_id "22" :value "85.7" :date "2013/14"
             :period_of_coverage "July 2013 to March 2014"
             :start_date "2013-07-01" :end_date "2014-03-31"}
            {:indicator_id "22" :value "86.7" :date "2012/13"
             :period_of_coverage "July 2012 to March 2013"
             :start_date "2012-07-01" :end_date "2013-03-31"}
            {:indicator_id "22" :value "88.3" :date "2011/12"
             :period_of_coverage "July 2011 to March 2012"
             :start_date "2011-07-01" :end_date "2012-03-31"}]
           (transform/enrich-dataset
            {:indicator-id "22"
             :fields-to-extract [:indicator_value :year :period_of_coverage]
             :fields-to-rename {:indicator_value :value :year :date}
             :conditions [{:field :level :values #{"England"}}]
             :format {:percentage :none}
             :resource-id "2b10e7b4-c799-44f9-81d6-3a42f4260893"}
            [{:year "2013/14" :indicator_value "85.7"
              :period_of_coverage "July 2013 to March 2014"}
             {:year "2012/13" :indicator_value "86.7"
              :period_of_coverage "July 2012 to March 2013"}
             {:year "2011/12" :indicator_value "88.3"
              :period_of_coverage "July 2011 to March 2012"}])))
    (is (= [{:end_date "2014-03-31", :start_date "2013-07-01", :value "0.8569999694824219",
             :date "2013/14", :indicator_id "22", :period_of_coverage "July 2013 to March 2014"}
            {:end_date "2013-03-31", :start_date "2012-07-01", :value "0.8669999694824219",
             :date "2012/13", :indicator_id "22", :period_of_coverage "July 2012 to March 2013"}
            {:end_date "2012-03-31", :start_date "2011-07-01", :value "0.8830000305175781",
             :date "2011/12", :indicator_id "22", :period_of_coverage "July 2011 to March 2012"}]
           (transform/enrich-dataset
            {:indicator-id "22"
             :fields-to-rename {:year :date :indicator_value :value}
             :conditions [{:field :level :values #{"Scotland"}}]
             :format {:percentage :divide}
             :resource-id "2b10e7b4-c799-44f9-81d6-3a42f4260893"}
            [{:year "2013/14" :indicator_value "85.7" 
              :period_of_coverage "July 2013 to March 2014"}
             {:year "2012/13" :indicator_value "86.7"
              :period_of_coverage "July 2012 to March 2013"}
             {:year "2011/12" :indicator_value "88.3" 
              :period_of_coverage "July 2011 to March 2012"}])))))

(deftest all-fields-exist?-test
  (testing "Testing function to check whether all fields a"
    (is (transform/all-fields-exist? [:year :id] {:year "2014" :id "5" :value 67}))
    (is (not (transform/all-fields-exist? [:year :id] {:year "2014" :value 67})))))

(deftest sum-sequence-test
  (testing "Testing adding up values for a given key"
    (is (= {:year "2012/13" :sum 50263.6 :period_of_coverage "July 2012 to March 2013"}
           (transform/sum-sequence
            :denominator
            [:denominator]
            [{:denominator "49962.0" :period_of_coverage "July 2012 to March 2013" :year "2012/13"}
             {:denominator "301.6" :period_of_coverage "July 2012 to March 2013" :year "2012/13"}])))
    (is (= {:year "2012" :sum 15 :period_of_coverage nil}
           (transform/sum-sequence
            :denominator
            [:denominator]
            [{:denominator "10" :period_of_coverage nil :year "2012"}
             {:denominator "5" :period_of_coverage nil :year "2012"}])))
    (is (= {:year "2012" :sum nil}
           (transform/sum-sequence
            :denominator
            [:denominator]
            [{:denominator "" :year "2012"}
             {:denominator "" :year "2012"}])))
    (is (= {:year "2012" :sum nil}
           (transform/sum-sequence
            :denominator
            [:denominator]
            [{:denominator nil :year "2012"}
             {:denominator nil :year "2012"}])))
    (is (= {:year "2012" :sum 1}
           (transform/sum-sequence
            :denominator
            [:denominator]
            [{:denominator "1" :year "2012"}
             {:denominator nil :year "2012"}])))))

(deftest divide-test
  (testing "Testing division."
    (is (= 5.0
           (transform/divide 25 5)))
    (is  (nil?
          (transform/divide 25 0)))
    (is (zero?
         (transform/divide 0 1)))
    (is (nil?
         (transform/divide nil 1)))
    (is (nil?
         (transform/divide nil nil)))
    (is (nil?
         (transform/divide 1 nil)))))

(deftest calculate-lci-uci-test
  (testing "Testing the calculation of CIs."
    (is (= [{:lens_value "England", :date "2011-12-09T00:00:00",
             :uci "0.42766399999999993", :lci "0.405", :indicator_id "33",
             :value "0.416332", :end_date "2012-03-31", :start_date "2011-04-01",
             :sub_lens_resource_id "", :period_of_coverage "1/4/2011 to 31/3/2012"}
            {:lens_value "England", :date "2010-11-09T00:00:00", :indicator_id "33",
             :value "0.405", :end_date "2011-03-31", :start_date "2010-04-01",
             :sub_lens_resource_id "", :period_of_coverage "1/4/2010 to 31/3/2011"}
            {:lens_value "England", :date "2009-10-09T00:00:00", :indicator_id "33",
             :value "0.411", :end_date "2010-03-31", :start_date "2009-04-01",
             :sub_lens_resource_id "", :period_of_coverage "1/4/2009 to 31/3/2010"}]
           (transform/calculate-lci-uci
            true
            [{:end_date "2012-03-31", :start_date "2011-04-01", :value "0.416332",
              :date "2011-12-09T00:00:00", :indicator_id "33", :sub_lens_resource_id "",
              :lens_value "England", :period_of_coverage "1/4/2011 to 31/3/2012"}
             {:end_date "2011-03-31", :start_date "2010-04-01", :value "0.405",
              :date "2010-11-09T00:00:00", :indicator_id "33", :sub_lens_resource_id "",
              :lens_value "England", :period_of_coverage "1/4/2010 to 31/3/2011"}
             {:end_date "2010-03-31", :start_date "2009-04-01", :value "0.411",
              :date "2009-10-09T00:00:00", :indicator_id "33", :sub_lens_resource_id "",
              :lens_value "England", :period_of_coverage "1/4/2009 to 31/3/2010"}])))
    (is (= [{:end_date "2012-03-31", :start_date "2011-04-01", :value "0.416332",
             :date "2011-12-09T00:00:00", :indicator_id "33", :sub_lens_resource_id "",
             :lens_value "England", :period_of_coverage "1/4/2011 to 31/3/2012"}
            {:end_date "2011-03-31", :start_date "2010-04-01", :value "0.405",
             :date "2010-11-09T00:00:00", :indicator_id "33", :sub_lens_resource_id "",
             :lens_value "England", :period_of_coverage "1/4/2010 to 31/3/2011"}
            {:end_date "2010-03-31", :start_date "2009-04-01", :value "0.411",
             :date "2009-10-09T00:00:00", :indicator_id "33", :sub_lens_resource_id "",
             :lens_value "England", :period_of_coverage "1/4/2009 to 31/3/2010"}]
           (transform/calculate-lci-uci
            false
            [{:end_date "2012-03-31", :start_date "2011-04-01", :value "0.416332",
              :date "2011-12-09T00:00:00", :indicator_id "33", :sub_lens_resource_id "",
              :lens_value "England", :period_of_coverage "1/4/2011 to 31/3/2012"}
             {:end_date "2011-03-31", :start_date "2010-04-01", :value "0.405",
              :date "2010-11-09T00:00:00", :indicator_id "33", :sub_lens_resource_id "",
              :lens_value "England", :period_of_coverage "1/4/2010 to 31/3/2011"}
             {:end_date "2010-03-31", :start_date "2009-04-01", :value "0.411",
              :date "2009-10-09T00:00:00", :indicator_id "33", :sub_lens_resource_id "",
              :lens_value "England", :period_of_coverage "1/4/2009 to 31/3/2010"}])))
    (is (= [{:end_date "2012-03-31", :start_date "2011-04-01", :value "0.416332",
             :date "2011-12-09T00:00:00", :indicator_id "33", :sub_lens_resource_id "",
             :lens_value "England", :period_of_coverage "1/4/2011 to 31/3/2012"}
            {:end_date "2011-03-31", :start_date "2010-04-01", :value "0.405",
             :date "2010-11-09T00:00:00", :indicator_id "33", :sub_lens_resource_id "",
             :lens_value "England", :period_of_coverage "1/4/2010 to 31/3/2011"}
            {:end_date "2010-03-31", :start_date "2009-04-01", :value "0.411",
             :date "2009-10-09T00:00:00", :indicator_id "33", :sub_lens_resource_id "",
             :lens_value "England", :period_of_coverage "1/4/2009 to 31/3/2010"}]
           (transform/calculate-lci-uci
            nil
            [{:end_date "2012-03-31", :start_date "2011-04-01", :value "0.416332",
              :date "2011-12-09T00:00:00", :indicator_id "33", :sub_lens_resource_id "",
              :lens_value "England", :period_of_coverage "1/4/2011 to 31/3/2012"}
             {:end_date "2011-03-31", :start_date "2010-04-01", :value "0.405",
              :date "2010-11-09T00:00:00", :indicator_id "33", :sub_lens_resource_id "",
              :lens_value "England", :period_of_coverage "1/4/2010 to 31/3/2011"}
             {:end_date "2010-03-31", :start_date "2009-04-01", :value "0.411",
              :date "2009-10-09T00:00:00", :indicator_id "33", :sub_lens_resource_id "",
              :lens_value "England", :period_of_coverage "1/4/2009 to 31/3/2010"}])))))
