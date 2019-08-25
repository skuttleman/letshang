(ns com.ben-allred.letshang.common.utils.dates
  (:refer-clojure :exclude [format])
  (:require
    #?@(:cljs [[java.time :refer [LocalDate LocalDateTime]]
               [java.time.format :refer [DateTimeFormatter]]])
    [cljc.java-time.day-of-week :as dow]
    [cljc.java-time.format.date-time-formatter :as dtf]
    [cljc.java-time.instant :as inst]
    [cljc.java-time.local-date :as ld]
    [cljc.java-time.local-date-time :as ldt]
    [cljc.java-time.zone-id :as zi]
    [cljc.java-time.zone-offset :as zo]
    [cljc.java-time.zoned-date-time :as zdt]
    [clojure.string :as string]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [tick.format :as tf]
    tick.locale-en-us)
  #?(:clj
     (:import
       (java.io Writer)
       (java.time LocalDate LocalDateTime)
       (java.time.format DateTimeFormatter)
       (java.util Date))))

#?(:cljs
   (do
     (extend-protocol IEquiv
       LocalDate
       (-equiv [this other]
         (and (some? other) (.equals this other)))

       LocalDateTime
       (-equiv [this other]
         (and (some? other) (.equals this other))))

     (extend-protocol IComparable
       LocalDate
       (-compare [this other]
         (ld/compare-to this other))

       LocalDateTime
       (-compare [this other]
         (ldt/compare-to this other)))))

(def ^:private formats
  {:datetime/view     "EEE, MMM d, yyyy h:mm a"
   :date/system       "yyyy-MM-dd"
   :date/view         "EEE, MMM d, yyyy"
   :date.no-year/view "EEE, MMM d"
   :datetime/fs       "yyyyMMddHHmmss"
   :date/year         "yyyy"
   :date/day          "d"
   :date/month        "MMM"})

(defn ^:private ->date [v]
  (cond
    (or (instance? LocalDateTime v) (instance? LocalDate v)) v
    (string? v) (ldt/parse v)
    (inst? v) (ldt/of-instant (inst/of-epoch-milli (.getTime v)) zo/utc)))

(defn ^:private ->ldt [v]
  (let [d (->date v)]
    (cond-> d
      (instance? LocalDate d) (ld/at-start-of-day))))

(defn format
  ([inst]
   (format inst :datetime/view))
  ([inst fmt]
   (let [inst' (if (instance? LocalDate inst)
                 inst
                 (zdt/of-instant (ldt/to-instant (->ldt inst) zo/utc) (zi/system-default)))]
     (-> fmt
         (formats fmt)
         (tf/formatter)
         (dtf/format inst')))))

(defn plus [inst? amt interval]
  (let [d (->date inst?)]
    (if (instance? LocalDate d)
      (cond-> d
        (= :years interval) (ld/plus-years amt)
        (= :months interval) (ld/plus-months amt)
        (= :weeks interval) (ld/plus-weeks amt)
        (= :days interval) (ld/plus-days amt))
      (cond-> d
        (= :years interval) (ldt/plus-years amt)
        (= :months interval) (ldt/plus-months amt)
        (= :weeks interval) (ldt/plus-weeks amt)
        (= :days interval) (ldt/plus-days amt)
        (= :hours interval) (ldt/plus-hours amt)
        (= :minutes interval) (ldt/plus-minutes amt)
        (= :seconds interval) (ldt/plus-seconds amt)))))

(defn minus [inst? amt interval]
  (plus inst? (* -1 amt) interval))

(defn with [inst? amt interval]
  (let [d (->date inst?)]
    (if (instance? LocalDate d)
      (cond-> d
              (= :year interval) (ld/with-year amt)
              (= :month interval) (ld/with-month amt)
              (= :day interval) (ld/with-day-of-month amt))
      (cond-> d
              (= :year interval) (ldt/with-year amt)
              (= :month interval) (ldt/with-month amt)
              (= :day interval) (ldt/with-day-of-month amt)
              (= :hour interval) (ldt/with-hour amt)
              (= :minute interval) (ldt/with-minute amt)
              (= :second interval) (ldt/with-second amt)))))

(defn year [inst]
  (format inst :date/year))

(defn month [inst]
  (format inst :date/month))

(defn day [inst]
  (format inst :date/day))

(defn day-of-week [inst?]
  (-> inst?
      (->ldt)
      (ldt/get-day-of-week)
      ({dow/sunday    :sunday
        dow/monday    :monday
        dow/tuesday   :tuesday
        dow/wednesday :wednesday
        dow/thursday  :thursday
        dow/friday    :friday
        dow/saturday  :saturday})))

(defn after? [date-1 date-2]
  (-> date-1
      (->ldt)
      (ldt/is-after (->ldt date-2))))

(defn before? [date-1 date-2]
  (-> date-1
      (->ldt)
      (ldt/is-before (->ldt date-2))))

(defn now []
  (ldt/now zo/utc))

(defn today []
  (ld/now))

(defn inst->ms [inst?]
  (inst/to-epoch-milli (ldt/to-instant (->ldt inst?) zo/utc)))

(defn date? [value]
  (or (instance? LocalDate value)
      (instance? LocalDateTime value)))

(defn relative [inst]
  (let [now (if (instance? LocalDate inst)
              (today)
              (now))
        inst' (->date inst)]
    (cond
      (= (format now :date/system) (format inst' :date/system))
      "Today"

      (= (format now :date/system) (format (minus inst' 1 :days) :date/system))
      "Tomorrow"

      (not= (year now) (year inst'))
      (format inst' :date/view)

      (and (after? inst' now) (before? (minus inst' 6 :days) now))
      (str "This " (string/capitalize (name (day-of-week inst'))))

      (and (after? inst' now) (before? (minus inst' 13 :days) now))
      (str "Next " (string/capitalize (name (day-of-week inst'))))

      :else
      (format inst' :date.no-year/view))))
