(ns kixi.nhs.data.date-test
  (:use clojure.test)
  (:require [kixi.nhs.data.date :refer :all]))

(deftest s->timestamp-test
  (testing "Testing parsing of dates."
    (is (= {:start_date "2013-04-06" :end_date "2014-04-05"}
           (s->uniform-date "2013-14" "\\d{4}-\\d{2}")))
    (is (= {:start_date "2013-04-06" :end_date "2014-04-05"}
           (s->uniform-date "2013/14" "\\d{4}\\/\\d{2}")))
    (is (= {:start_date "2013-01-01" :end_date "2013-12-31"}
           (s->uniform-date "2013" "\\d{4}")))
    (is (= {:start_date "2013-10-01" :end_date "2013-12-31"}
           (s->uniform-date "Oct - Dec 2013" "\\w* - \\w* \\d{4}")))
    (is (= {:start_date "2013-04-01" :end_date "2014-03-31"}
           (s->uniform-date "1/4/2013 to 31/3/2014" "\\d{1,2}/\\d{1,2}/\\d{4} to \\d{1,2}/\\d{1,2}/\\d{4}")))
    (is (= {:start_date "2014-01-01" :end_date "2014-02-28"}
           (s->uniform-date "January 2014 to February 2014" "\\w* \\d{4} to \\w* \\d{4}")))
    (is (= {:start_date "2015-01-01" :end_date "2015-01-01"}
           (s->uniform-date "2015-01-01T00:00:00" "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")))
    (is (= {:start_date "2015-02-01" :end_date "2015-02-28"}
           (s->uniform-date "2015 February" "\\d{4} \\w+")))))

(deftest uniform-date-test
  (testing "Testing parse-date."
    (is (= {:start_date "2013-04-06"
            :end_date "2014-04-05"
            :period_of_coverage "2013-14"}
           (uniform-dates {:period_of_coverage "2013-14"})))
    (is (= {:period_of_coverage "2013-14"
            :start_date "2013-04-06"
            :end_date "2014-04-05"}
           (uniform-dates {:period_of_coverage "2013-14"})))
    (is (= {:period_of_coverage "2013"
            :start_date "2013-01-01"
            :end_date "2013-12-31"}
           (uniform-dates {:period_of_coverage "2013"})))
    (is (= {:period_of_coverage "Oct - Dec 2013"
            :start_date "2013-10-01"
            :end_date "2013-12-31"}
           (uniform-dates {:period_of_coverage "Oct - Dec 2013"})))
    (is (= {:period_of_coverage "1/4/2013 to 31/3/2014"
            :start_date "2013-04-01"
            :end_date "2014-03-31"}
           (uniform-dates {:period_of_coverage "1/4/2013 to 31/3/2014"})))
    (is (= {:period_of_coverage "January 2014 to February 2014"
            :start_date "2014-01-01"
            :end_date "2014-02-28"}
           (uniform-dates {:period_of_coverage "January 2014 to February 2014"})))
    (is (= {:period_of_coverage "2015-12-13T00:00:00"
            :start_date "2015-12-13"
            :end_date "2015-12-13"}
           (uniform-dates {:period_of_coverage "2015-12-13T00:00:00"})))
    (is (= {:period_of_coverage "2015 January"
            :start_date "2015-01-01"
            :end_date "2015-01-31"}
           (uniform-dates {:period_of_coverage "2015 January"})))))
