(ns com.ben-allred.letshang.common.utils.dates
  (:refer-clojure :exclude [format])
  #?@(:clj
      [(:import
         (java.time Instant ZonedDateTime ZoneId ZoneOffset)
         (java.time.chrono ChronoLocalDateTime ChronoZonedDateTime)
         (java.time.format DateTimeFormatter DateTimeParseException)
         (java.util Date))]
      :cljs
      [(:require
         [goog.date :as gdate])
       (:import
         (goog.date DateTime)
         (goog.i18n DateTimeFormat))]))

(def ^:private default-format
  "EEE MMM d, yyyy 'at' h:mm a")

(defn ^:private inst->dt [inst]
  #?(:clj  (if (instance? Instant inst)
             inst
             (.toInstant inst))
     :cljs (if (instance? DateTime inst)
             inst
             (DateTime. inst))))

(defn format
  ([inst]
   (format inst default-format))
  ([inst fmt]
    #?(:clj  (-> fmt
                 (DateTimeFormatter/ofPattern)
                 (.withZone (ZoneId/systemDefault))
                 (.format (inst->dt inst)))
       :cljs (.format (DateTimeFormat. fmt) (inst->dt inst)))))

(defn inst-str? [s]
  (boolean (and (string? s)
                (re-matches #"\d{4}-[0-1][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9](\.\d{3})?(Z|(\+|-)\d{2}:\d{2})" s)
                #?(:clj  (try
                           (ZonedDateTime/parse s)
                           (catch DateTimeParseException ex
                             false))
                   :cljs (gdate/fromIsoString s)))))
(defn ->inst [v]
  (cond
    (inst? v)
    v

    #?@(:clj  [(instance? Instant v)
               (Date/from v)

               (instance? ChronoZonedDateTime v)
               (->inst (.toInstant v))

               (instance? ChronoLocalDateTime v)
               (->inst (.toInstant v ZoneOffset/UTC))]
        :cljs [(instance? DateTime v)
               (.-date v)])

    (inst-str? v)
    #?(:clj  (->inst (ZonedDateTime/parse v))
       :cljs (->inst (gdate/fromIsoString v)))))
