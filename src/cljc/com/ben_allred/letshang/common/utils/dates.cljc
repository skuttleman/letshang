(ns com.ben-allred.letshang.common.utils.dates
  (:refer-clojure :exclude [format])
  (:require
    #?(:cljs [goog.date :as gdate])
    [com.ben-allred.letshang.common.utils.numbers :as numbers])
  #?(:clj  (:import
             (java.time DayOfWeek Instant LocalDate LocalDateTime LocalTime ZonedDateTime ZoneOffset)
             (java.time.chrono ChronoLocalDate ChronoLocalDateTime ChronoZonedDateTime)
             (java.time.format DateTimeFormatter DateTimeParseException)
             (java.util Date))
     :cljs (:import
             (goog.date Date DateTime)
             (goog.i18n DateTimeFormat))))

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
  {:datetime/view "EEE MMM d, yyyy 'at' h:mm a"
   :date/system   "yyyy-MM-dd"
   :date/view     "EEE MMM d, yyyy"
   :datetime/fs   "yyyyMMddHHmmss"
   :date/year     "yyyy"
   :date/day      "d"
   :date/month    "MMMM"})

(defn ^:private inst->dt [inst]
  #?(:clj  (if (instance? Instant inst)
             inst
             (.toInstant (->inst inst)))
     :cljs (if (instance? DateTime inst)
             inst
             (DateTime. (->inst inst)))))

(defn format
  ([inst]
   (format inst :datetime/view))
  ([inst fmt]
    #?(:clj  (-> fmt
                 (formats fmt)
                 (DateTimeFormatter/ofPattern)
                 (.withZone ZoneOffset/UTC)
                 (.format (inst->dt inst)))
       :cljs (-> fmt
                 (formats fmt)
                 (DateTimeFormat.)
                 (.format (inst->dt inst))))))

(defn inst-str? [s]
  (boolean (and (string? s)
                (re-matches #"\d{4}-[0-1][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9](\.\d{3})?(Z|(\+|-)\d{2}:\d{2})" s)
                #?(:clj  (try
                           (ZonedDateTime/parse s)
                           (catch DateTimeParseException _
                             false))
                   :cljs (gdate/fromIsoString s)))))

(defn ->inst [v]
  (cond
    #?@(:clj  [(instance? Instant v)
               (Date/from v)

               (instance? ChronoZonedDateTime v)
               (->inst (.toInstant v))

               (instance? ChronoLocalDateTime v)
               (->inst (.toInstant v ZoneOffset/UTC))

               (instance? ChronoLocalDate v)
               (->inst (.atTime v (LocalTime/of 0 0 0 0)))]
        :cljs [(or (instance? DateTime v) (instance? Date v))
               (.-date v)])

    (inst? v)
    v

    (inst-str? v)
    #?(:clj  (->inst (ZonedDateTime/parse v))
       :cljs (->inst (gdate/fromIsoString v)))))

(defn plus [inst? amt interval]
  #?(:clj  (-> inst?
               (inst->dt)
               (LocalDateTime/ofInstant ZoneOffset/UTC)
               (cond->
                 (= :years interval) (.plusYears amt)
                 (= :months interval) (.plusMonths amt)
                 (= :weeks interval) (.plusWeeks amt)
                 (= :days interval) (.plusDays amt)
                 (= :hours interval) (.plusHours amt)
                 (= :minutes interval) (.plusMinutes amt)
                 (= :seconds interval) (.plusSeconds amt))
               (.toInstant ZoneOffset/UTC))
     :cljs (doto (.clone (inst->dt inst?))
             (.add (-> {:years 0 :months 0 :weeks 0 :days 0 :hours 0 :minutes 0 :seconds 0}
                       (assoc interval amt)
                       (clj->js))))))

(defn minus [inst? amt interval]
  (plus inst? (* -1 amt) interval))

(defn with [inst? amt interval]
  #?(:clj  (-> inst?
               (inst->dt)
               (LocalDateTime/ofInstant ZoneOffset/UTC)
               (cond->
                 (= :year interval) (.withYear amt)
                 (= :month interval) (.withMonth amt)
                 (= :day interval) (.withDayOfMonth amt)
                 (= :hour interval) (.withHour amt)
                 (= :minute interval) (.withMinute amt)
                 (= :second interval) (.withSecond amt))
               (.toInstant ZoneOffset/UTC))
     :cljs (doto (.clone (inst->dt inst?))
             (cond->
               (= :year interval) (.setFullYear amt)
               (= :month interval) (.setMonth (dec amt))
               (= :day interval) (.setDate amt)
               (= :hour interval) (.setHours amt)
               (= :minute interval) (.setMinutes amt)
               (= :second interval) (.setSeconds amt)))))

(defn year [inst]
  (format inst :date/year))

(defn month [inst]
  (format inst :date/month))

(defn day [inst]
  (format inst :date/day))

(defn day-of-week [inst?]
  #?(:clj (-> inst?
              (inst->dt)
              (LocalDateTime/ofInstant ZoneOffset/UTC)
              (.getDayOfWeek)
              ({DayOfWeek/SUNDAY    :sunday
                DayOfWeek/MONDAY    :monday
                DayOfWeek/TUESDAY   :tuesday
                DayOfWeek/WEDNESDAY :wednesday
                DayOfWeek/THURSDAY  :thursday
                DayOfWeek/FRIDAY    :friday
                DayOfWeek/SATURDAY  :saturday}))
     :cljs (-> inst?
               (inst->dt)
               (.getWeekday)
               ([:sunday :monday :tuesday :wednesday :thursday :friday :saturday]))))

(defn after? [date-1 date-2]
  #?(:clj  (-> date-1
               (inst->dt)
               (LocalDateTime/ofInstant ZoneOffset/UTC)
               (.isAfter (LocalDateTime/ofInstant (inst->dt date-2) ZoneOffset/UTC)))
     :cljs (-> date-1
               (inst->dt)
               (as-> $ (.compare Date $ (inst->dt date-2)))
               (pos?))))

(defn before? [date-1 date-2]
  #?(:clj  (-> date-1
               (inst->dt)
               (LocalDateTime/ofInstant ZoneOffset/UTC)
               (.isBefore (LocalDateTime/ofInstant (inst->dt date-2) ZoneOffset/UTC)))
     :cljs (-> date-1
               (inst->dt)
               (as-> $ (.compare Date $ (inst->dt date-2)))
               (neg?))))

(defn now []
  #?(:clj  (Instant/now)
     :cljs (DateTime.)))

(defn today []
  #?(:clj  (LocalDate/ofInstant (now) ZoneOffset/UTC)
     :cljs (Date.)))

(defn inst->ms [inst?]
  (.getTime (->inst inst?)))

(defn date? [value]
  (or (instance? #?(:clj LocalDate :cljs Date) value)
      (instance? #?(:clj LocalDateTime :cljs DateTime) value)))
