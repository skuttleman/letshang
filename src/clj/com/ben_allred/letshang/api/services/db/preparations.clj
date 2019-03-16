(ns com.ben-allred.letshang.api.services.db.preparations
  (:require
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.strings :as strings]
    [com.ben-allred.letshang.common.utils.dates :as dates])
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

(def moments-window (for-type :moments-window))

(def user-response (for-type :user-response))
