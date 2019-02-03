(ns com.ben-allred.letshang.api.services.db.models.shared
  (:require
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]))

(defprotocol Model
  (->db [this data])
  (->api [this data]))

(defn under [root-key]
  (let [root-key' (name root-key)]
    (map (fn [item]
           (let [groups (group-by (comp namespace first) item)
                 others (dissoc groups nil root-key')
                 init (into {} (concat (get groups nil) (get groups root-key')))]
             (->> others
                  (reduce (fn [m [k v]] (assoc m k (into {} v))) init)))))))

(defn select
  ([query model]
   (select query model identity))
  ([query model x-form]
   (-> [query]
       (conj (comp (map (partial ->api model)) x-form))
       (repos/single))))
