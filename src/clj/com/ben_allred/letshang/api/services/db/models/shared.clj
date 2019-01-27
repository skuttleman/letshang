(ns com.ben-allred.letshang.api.services.db.models.shared
  (:require
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]))

(defprotocol Model
  (->db [this data])
  (->api [this data]))

(defn select [query model]
  (-> [query]
      (conj (partial map (partial ->api model)))
      (repos/single)))
