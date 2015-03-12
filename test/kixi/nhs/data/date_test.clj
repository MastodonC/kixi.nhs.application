(ns kixi.nhs.data.date-test
  (:use clojure.test)
  (:require [kixi.nhs.data.date :refer :all]))

(deftest s->timestamp-test
  (testing "Testing parsing of dates."
    (is (= "2013-14"
           (s->uniform-date "2013-14" "\\d{4}-\\d{2}")))
    (is (= "2013-14"
           (s->uniform-date "2013/14" "\\d{4}\\/\\d{2}")))
    (is (= "2013"
           (s->uniform-date "2013" "\\d{4}")))
    (is (= "1-10-2013 1-12-2013"
           (s->uniform-date "Oct - Dec 2013" "\\w* - \\w* \\d{4}")))
    (is (= "1-4-2013 31-3-2014"
           (s->uniform-date "1/4/2013 to 31/3/2014" "\\d{1,2}/\\d{1,2}/\\d{4} to \\d{1,2}/\\d{1,2}/\\d{4}")))
    (is (= "1-1-2014 1-2-2014"
           (s->uniform-date "January 2014 to February 2014" "\\w* \\d{4} to \\w* \\d{4}")))))

(deftest uniform-date-test
  (testing "Testing parse-date."
    (is (= {:date "2013-14"}
           (uniform-date {:date "2013-14"} :date)))
    (is (= {:date "2013-14"}
           (uniform-date {:date "2013-14"} :date)))
    (is (= {:date "2013"}
           (uniform-date {:date "2013"} :date)))
    (is (= {:date "1-10-2013 1-12-2013"}
           (uniform-date {:date "Oct - Dec 2013"} :date)))
    (is (= {:date "1-4-2013 31-3-2014"}
           (uniform-date {:date "1/4/2013 to 31/3/2014"} :date)))
    (is (= {:date "1-1-2014 1-2-2014"}
           (uniform-date {:date "January 2014 to February 2014"} :date)))))
