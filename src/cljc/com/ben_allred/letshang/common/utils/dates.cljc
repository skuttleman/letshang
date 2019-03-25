(ns com.ben-allred.letshang.common.utils.dates
  (:refer-clojure :exclude [format])
  (:require
    #?@(:cljs [[cljs-time.core :as time]
               [cljs-time.coerce :as time.coerce]
               [cljs-time.format :as time.format]])
    [clojure.string :as string]
    [com.ben-allred.letshang.common.utils.logging :as log])
  #?(:clj
     (:import
       (java.time DayOfWeek LocalDate LocalDateTime LocalTime ZonedDateTime ZoneOffset)
       (java.time.format DateTimeFormatter)
       (java.util Date))
     :cljs
     (:import
       (goog.date Date DateTime))))

#?(:cljs
   (extend-protocol IEquiv
     Date
     (-equiv [this other]
       (cond
         (nil? other) false
         (instance? Date other) (.equals this other)
         :else (.equals this (Date. other))))

     DateTime
     (-equiv [this other]
       (cond
         (nil? other) false
         (instance? DateTime other) (.equals this other)
         :else (.equals this (DateTime. other))))))

(declare ->inst)

(def ^:private formats
  {:datetime/view     "EEE, MMM d, yyyy 'at' h:mm a"
   :date/system       "yyyy-MM-dd"
   :date/view         "EEE, MMM d, yyyy"
   :date.no-year/view "EEE, MMM d"
   :datetime/fs       "yyyyMMddHHmmss"
   :date/year         "yyyy"
   :date/day          "d"
   :date/month        "MMM"})

(defn ^:private at-midnight [local-date]
  #?(:clj  (LocalDateTime/of local-date (LocalTime/of 0 0 0))
     :cljs (time/at-midnight local-date)))

(defn ^:private ->internal [v]
  #?(:clj  (cond
             (instance? LocalDate v) (at-midnight v)
             (instance? LocalDateTime v) v
             (string? v) (LocalDateTime/parse v)
             (inst? v) (LocalDateTime/ofInstant (.toInstant v) ZoneOffset/UTC))
     :cljs (cond
             (time/date? v) (time.coerce/to-date-time v)
             (string? v) (time.format/parse-local v)
             (inst? v) (time.coerce/from-date v))))

(defn format
  ([inst]
   (format inst :datetime/view))
  ([inst fmt]
    #?(:clj  (-> fmt
                 (formats fmt)
                 (DateTimeFormatter/ofPattern)
                 (.withZone ZoneOffset/UTC)
                 (.format (->internal inst)))
       :cljs (-> fmt
                 (formats fmt)
                 (->> (hash-map :format-str))
                 (time.format/unparse (time/to-default-time-zone (->internal inst)))))))

(defn ->inst [v]
  #?(:clj  (cond
             (instance? LocalDate v) (Date/from (.toInstant (at-midnight v) ZoneOffset/UTC))
             (instance? LocalDateTime v) (Date/from (.toInstant v ZoneOffset/UTC))
             (string? v) (Date/from (.toInstant (ZonedDateTime/parse v)))
             (inst? v) v)
     :cljs (cond
             (time/date? v) (.-date (time.coerce/to-date-time v))
             (string? v) (.-date (time.coerce/to-date-time (time.format/parse v)))
             (inst? v) v)))

(defn plus [inst? amt interval]
  #?(:clj  (-> inst?
               (->internal)
               (cond->
                 (= :years interval) (.plusYears amt)
                 (= :months interval) (.plusMonths amt)
                 (= :weeks interval) (.plusWeeks amt)
                 (= :days interval) (.plusDays amt)
                 (= :hours interval) (.plusHours amt)
                 (= :minutes interval) (.plusMinutes amt)
                 (= :seconds interval) (.plusSeconds amt)))
     :cljs (-> inst?
               (->internal)
               (time/plus (time/period interval amt)))))

(defn minus [inst? amt interval]
  (plus inst? (* -1 amt) interval))

(defn with [inst? amt interval]
  #?(:clj  (-> inst?
               (->internal)
               (cond->
                 (= :year interval) (.withYear amt)
                 (= :month interval) (.withMonth amt)
                 (= :day interval) (.withDayOfMonth amt)
                 (= :hour interval) (.withHour amt)
                 (= :minute interval) (.withMinute amt)
                 (= :second interval) (.withSecond amt))
               (->inst))
     :cljs (let [m {interval amt}
                 dt (->internal inst?)]
             (-> (time/date-time (:year m (.getYear dt))
                                 (:month m (inc (.getMonth dt)))
                                 (:day m (.getDate dt))
                                 (:hour m (.getHours dt))
                                 (:minute m (.getMinutes dt))
                                 (:second m (.getSeconds dt))
                                 (.getUTCMilliseconds dt))
                 (->inst)))))

(defn year [inst]
  (format inst :date/year))

(defn month [inst]
  (format inst :date/month))

(defn day [inst]
  (format inst :date/day))

(defn day-of-week [inst?]
  #?(:clj  (-> inst?
               (->internal)
               (.getDayOfWeek)
               ({DayOfWeek/SUNDAY    :sunday
                 DayOfWeek/MONDAY    :monday
                 DayOfWeek/TUESDAY   :tuesday
                 DayOfWeek/WEDNESDAY :wednesday
                 DayOfWeek/THURSDAY  :thursday
                 DayOfWeek/FRIDAY    :friday
                 DayOfWeek/SATURDAY  :saturday}))
     :cljs (-> inst?
               (->internal)
               (time/day-of-week)
               (dec)
               ([:monday :tuesday :wednesday :thursday :friday :saturday :sunday]))))

(defn after? [date-1 date-2]
  #?(:clj  (-> date-1
               (->internal)
               (.isAfter (->internal date-2)))
     :cljs (time/after? (->internal date-1) (->internal date-2))))

(defn before? [date-1 date-2]
  #?(:clj  (-> date-1
               (->internal)
               (.isBefore (->internal date-2)))
     :cljs (time/before? (->internal date-1) (->internal date-2))))

(defn now []
  #?(:clj  (LocalDateTime/now ZoneOffset/UTC)
     :cljs (time/now)))

(defn today []
  #?(:clj  (.toLocalDate (now))
     :cljs (time/today-at-midnight)))

(defn inst->ms [inst?]
  (.getTime (->inst inst?)))

(defn date? [value]
  (or (inst? value)
      #?@(:clj  [(instance? LocalDate value)
                 (instance? LocalDateTime value)]
          :cljs [(time/date? value)])))

(defn relative [inst]
  (let [now (->inst (now))
        inst' (->inst inst)]
    (cond
      (= (format now :date/system) (format inst' :date/system))
      "Today"

      (not= (year now) (year inst'))
      (format inst' :date/view)

      (and (after? inst' now) (before? (minus inst' 1 :weeks) now))
      (str "This " (string/capitalize (name (day-of-week inst'))))

      :else
      (format inst' :date.no-year/view))))
