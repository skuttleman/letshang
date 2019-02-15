(ns com.ben-allred.letshang.api.services.db.preparations
  (:require
    [com.ben-allred.letshang.common.utils.strings :as strings])
  (:import
    (org.postgresql.util PGobject)))

(defn prepare [type]
  (let [type (strings/kebab->snake (name type))]
    (fn [value]
      (doto (PGobject.)
        (.setType type)
        (.setValue (name value))))))
