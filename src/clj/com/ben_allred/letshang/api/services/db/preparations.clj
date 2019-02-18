(ns com.ben-allred.letshang.api.services.db.preparations
  (:require
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.strings :as strings])
  (:import
    (org.postgresql.util PGobject)))

(defn for-type [type]
  (let [type (strings/kebab->snake (name type))]
    (fn [value]
      (doto (PGobject.)
        (.setType type)
        (.setValue (name value))))))

(defn prepare [->sql-value table]
  (partial into
           {}
           (map (fn [[k v]]
                  [(keywords/kebab->snake k) (->sql-value table k v)]))))
