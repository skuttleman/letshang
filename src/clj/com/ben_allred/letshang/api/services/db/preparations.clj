(ns com.ben-allred.letshang.api.services.db.preparations
  (:require [clojure.string :as string])
  (:import
    (org.postgresql.util PGobject)))

(defn prepare [type]
  (let [type (string/replace (name type) #"-" "_")]
    (fn [value]
      (doto (PGobject.)
        (.setType type)
        (.setValue (name value))))))
