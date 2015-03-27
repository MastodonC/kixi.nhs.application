(ns kixi.nhs.data.date
  (:require [clj-time.format  :as tf]
            [clj-time.core    :as t]
            [clojure.string   :as str]))


(def parsers [#"\d{4}"
              #"\w* \d{4} to \w* \d{4}"
              #"\w* - \w* \d{4}"
              #"\d{4}-\d{2}"
              #"\d{4}\/\d{2}"
              #"\d{1,2}/\d{1,2}/\d{4} to \d{1,2}/\d{1,2}/\d{4}"
              #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}"
              #"\d{4} \w+"
              #"\d{4}-\d{2}-\d{2}"
              #"\d{4}-\d{2}-\d{2} to \d{4}-\d{2}-\d{2}"
              #"\w+-\d{2}"
              #"Q\d \d{4}-\d{2}"])

(defn date->str [s]
  (tf/unparse (tf/formatter "yyyy-MM-dd") s))

(defn str->date [s formatter]
  (->> s str/trim (tf/parse formatter)))

(defmulti s->uniform-date (fn [date-str pattern] pattern))

;; 2013
(defmethod s->uniform-date "\\d{4}" [date-str pattern]
  {:start_date (str date-str "-01-01")
   :end_date   (str date-str "-12-31")})

;; Jun-14
(defmethod s->uniform-date "\\w+-\\d{2}" [date-str pattern]
  (let [formatter (tf/formatter "MMM-yy")
        start     (-> date-str (str->date formatter))
        end       (-> start t/last-day-of-the-month date->str)]
    {:start_date (date->str start)
     :end_date   end}))

;; 2013/14 => fiscal year 2013-04-06 - 2014-04-05
(defmethod s->uniform-date "\\d{4}\\/\\d{2}" [date-str pattern]
  (let [[start end] (str/split date-str #"/")]
    {:start_date (str start "-04-06")
     :end_date   (str (t/year (str->date end (tf/formatter "yy"))) "-04-05")}))

;; 2013-14 => fiscal year 2013-04-06 - 2014-04-05
(defmethod s->uniform-date "\\d{4}-\\d{2}" [date-str pattern]
  (let [[start end] (str/split date-str #"-")]
    {:start_date (str start "-04-06")
     :end_date   (str (t/year (str->date end (tf/formatter "yy"))) "-04-05")}))

;; 1/4/2013 to 31/3/2014 => {:start_date "2013-04-01" :end-date "2014-03-31"}
(defmethod s->uniform-date "\\d{1,2}/\\d{1,2}/\\d{4} to \\d{1,2}/\\d{1,2}/\\d{4}" [s pattern]
  (let [[start-str end-str] (str/split s #" to ")
        formatter           (tf/formatter "dd/MM/yyyy")
        start-date          (-> start-str (str->date formatter) date->str)
        end-date            (-> end-str (str->date formatter) date->str)]
    {:start_date start-date
     :end_date   end-date}))

;; January 2014 to February 2014 => {:start_date "2014-01-01" :end_date "2014-02-29"}
(defmethod s->uniform-date "\\w* \\d{4} to \\w* \\d{4}" [s pattern]
  (let [[start-str end-str] (str/split s #" to ")
        formatter           (tf/formatter "MMMM yyyy")
        start-date          (-> start-str (str->date formatter) date->str)
        end-date            (-> end-str (str->date formatter) t/last-day-of-the-month date->str)]
    {:start_date start-date
     :end_date end-date}))

;; Oct - Dec 2013 => {:start_date "2013-10-01" :end_date "2013-12-31"}
(defmethod s->uniform-date "\\w* - \\w* \\d{4}" [s pattern]
  (let [[start-str end-str] (str/split s #" - ")
        year                (first (re-seq #"\d{4}" end-str))
        formatter           (tf/formatter "MMM yyyy")
        start-date          (-> (str start-str " " year) (str->date formatter) date->str)
        end-date            (-> end-str (str->date formatter) t/last-day-of-the-month date->str)]
    {:start_date start-date
     :end_date end-date}))

;; 2015-12-13T00:00:00 => {:start_date "2015-12-13" :end_date "2015-12-13"}
(defmethod s->uniform-date "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}" [s pattern]
  (let [formatter    (tf/formatter "yyyy-MM-dd'T'HH:mm:ss")
        start-date   (-> s (str->date formatter) date->str)]
    {:start_date start-date
     :end_date start-date}))

;; 2015 October
(defmethod s->uniform-date "\\d{4} \\w+" [s pattern]
  (let [formatter    (tf/formatter "yyyy MMMM")
        start-date   (-> s (str->date formatter) date->str)
        end-date     (-> s (str->date formatter) t/last-day-of-the-month date->str)]
    {:start_date start-date
     :end_date end-date}))

;; 2014-07-01
(defmethod s->uniform-date "\\d{4}-\\d{2}-\\d{2}" [s pattern]
  (let [formatter    (tf/formatter "yyyy-MM-dd")
        start-date   (-> s (str->date formatter) date->str)
        end-date     (-> s (str->date formatter) t/last-day-of-the-month date->str)]
    {:start_date start-date
     :end_date end-date}))

;; 2015-04-06 to 2015-07-06
(defmethod s->uniform-date "\\d{4}-\\d{2}-\\d{2} to \\d{4}-\\d{2}-\\d{2}" [s pattern]
  (let [[start-date end-date] (str/split s #" to ")]
    {:start_date start-date
     :end_date end-date}))

;; Q1 2013-14 => :start_date "2013-04-06" :end_date "2013-06-05" 
(defmethod s->uniform-date "Q\\d \\d{4}-\\d{2}" [s pattern]
  (let [[quarter years] (str/split s #" ")]
    (cond (= quarter "Q1") {:start_date (str (subs years 0 4) "-04-06")
                            :end_date   (str (subs years 0 4) "-07-05")}
          (= quarter "Q2") {:start_date (str (subs years 0 4) "-07-06")
                            :end_date   (str (subs years 0 4) "-10-05")} 
          (= quarter "Q3") {:start_date (str (subs years 0 4) "-10-06")
                            :end_date   (str "20" (subs years 5) "-01-05")} 
          (= quarter "Q4") {:start_date (str "20" (subs years 5) "-01-06")
                            :end_date   (str "20" (subs years 5) "-04-05")})))

(defn parse
  "Takes parser and a string, and tries to find a match for its
  format. If match is found, it parses the date. Otherwise
  returns nil."
  [parser s]
  (when s
    (let [date-str (re-seq parser s)]
      (when (= s (first date-str)) ;; we're looking for exact match
        (s->uniform-date s (str parser))))))

(defn uniform-dates
  "Takes a map and parses key :period_of_coverage
  into {:start_date x :end_date x}
  Returns the same map, with dates associated into it
  if parsing was successful, otherwise the map
  is returned unchanged."
  [m]
  (let [start-end-dates (some (fn [p]
                                (let [d (parse p (:period_of_coverage m))]
                                  (when-not (nil? d)
                                    d))) parsers)]
    (if (seq start-end-dates)
      (merge m start-end-dates)
      m)))
