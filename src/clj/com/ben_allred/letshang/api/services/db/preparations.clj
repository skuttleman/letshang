(ns com.ben-allred.letshang.api.services.db.preparations
  (:refer-clojure :exclude [time])
  (:require
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.serde.json :as json]
    [com.ben-allred.letshang.common.utils.strings :as strings])
  (:import
    (org.postgresql.util PGobject)))

(defn ^:private for-type [type]
  (let [type (strings/kebab->snake (name type))]
    (fn [value]
      (doto (PGobject.)
        (.setType type)
        (.setValue (keywords/safe-name value))))))

(defn prepare [->sql-value table]
  (partial into
           {}
           (map (fn [[k v]]
                  [(keywords/kebab->snake k) (->sql-value table k v)]))))

(def date (comp (for-type :date) #(dates/format % :date/system)))

(def invitations-match-type (for-type :invitations-match-type))

(def jsonb (comp (for-type :jsonb) json/encode))

(def moments-window (for-type :moments-window))

(def time (for-type :time))

(def user-response (for-type :user-response))
