(ns com.ben-allred.letshang.api.services.db.models.shared
  (:require
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defmulti ->api (comp first vector))
(defmulti ->db (comp first vector))
(defmethod ->api :default [_ value] value)
(defmethod ->db :default [_ value] value)

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
   [query
    (comp (partial sequence (comp (map (partial ->api model)) x-form))
          peek)]))

(defn insert-many [query entity model]
  (update query :values (comp (partial map #(->db model (select-keys % (:fields entity))))
                              colls/force-sequential)))

(defn modify [query entity model]
  (update query :set #(->db model (select-keys % (:fields entity)))))
