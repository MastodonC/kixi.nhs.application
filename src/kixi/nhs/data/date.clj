(ns kixi.nhs.data.date
  (:require [clj-time.format  :as tf]
            [clj-time.core    :as t]
            [clojure.string   :as str]))


(def parsers [#"\d{4}"
              #"\w* \d{4} to \w* \d{4}"
              #"\w* - \w* \d{4}"
              #"\d{4}-\d{2}"
              #"\d{4}\/\d{2}"
              #"\d{1,2}/\d{1,2}/\d{4} to \d{1,2}/\d{1,2}/\d{4}"])

(def months
  ["January" "February" "March" "April" "May" "June" "July" "August"
   "September" "October" "November" "December"])

(def short-months
  ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug"
   "Sep" "Oct" "Nov" "Dec"])

(defn index-of [coll x]
  (first (keep-indexed #(when (= %2 x) %1) coll)))

(defmulti s->uniform-date (fn [date-str pattern] pattern))

;; 2013
(defmethod s->uniform-date "\\d{4}" [date-str pattern]
  date-str)

;; 2013/14
(defmethod s->uniform-date "\\d{4}\\/\\d{2}" [date-str pattern]
  (str/replace date-str #"/" "-"))

;; 2013-14
(defmethod s->uniform-date "\\d{4}-\\d{2}" [date-str pattern]
  date-str)

;; 01/01/2014
(defmethod s->uniform-date "\\d{4}-\\d{2}" [date-str pattern]
  date-str)

(defn- scrub [s]
  (-> s
      str/trim
      (str/replace #"/" "-")))

;; 1/4/2013 to 31/3/2014
(defmethod s->uniform-date "\\d{1,2}/\\d{1,2}/\\d{4} to \\d{1,2}/\\d{1,2}/\\d{4}" [s pattern]
  (let [[start-str end-str] (str/split s #" to ")
        start-date          (scrub (first (re-seq #"\d{1,2}/\d{1,2}/\d{4}" start-str)))
        end-date            (scrub (first (re-seq #"\d{1,2}/\d{1,2}/\d{4}" end-str)))]
    (str start-date " " end-date)))

;; January 2014 to February 2014
(defmethod s->uniform-date "\\w* \\d{4} to \\w* \\d{4}" [s pattern]
  (let [[start-str end-str] (str/split s #" to ")
        start-date          (str "1" "-"
                                 (inc (index-of months (str/trim (first (re-seq #"\w*" start-str))))) "-"
                                 (str/trim (first (re-seq #"\d{4}" start-str))))
        end-date            (str "1" "-"
                                 (inc (index-of months (str/trim (first (re-seq #"\w*" end-str))))) "-"
                                 (str/trim (first (re-seq #"\d{4}" end-str))))]
    (str start-date " " end-date)))

;; Oct - Dec 2013
(defmethod s->uniform-date "\\w* - \\w* \\d{4}" [s pattern]
  (let [[start-str end-str] (str/split s #" - ")
        start-date          (str "1" "-"
                                 (inc (index-of short-months (str/trim start-str))) "-"
                                 (str/trim (first (re-seq  #"\d{4}" end-str))))
        end-date            (str "1" "-"
                                 (inc (index-of short-months (str/trim (first (re-seq #"\w*" end-str))))) "-"
                                 (str/trim (first (re-seq #"\d{4}" end-str))))]
    (str start-date " " end-date)))

(defn parse
  "Takes parser and a string, and tries to find a match for its
  format. If match is found, it parses the date. Otherwise
  returns nil."
  [parser s]
  (when s
    (let [date-str (re-seq parser s)]
      (when (= s (first date-str)) ;; we're looking for exact match
        (s->uniform-date s (str parser))))))

(defn uniform-date
  "Takes a map and parses keys :date
  and :period_of_coverage ito  a uniform
  format.
  Returns the same map, with dates parsed if
  it parsing was successful, otherwise the dates
  remain unchanged."
  [m k]
  (let [date (some (fn [p]
                     (let [d (parse p (k m))]
                       (when-not (nil? d)
                         d))) parsers)]
    (if date
      (assoc-in m [k] date)
      m)))
