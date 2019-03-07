(ns com.ben-allred.letshang.api.services.db.models.shared
  (:require
    [com.ben-allred.letshang.api.services.db.preparations :as prep]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
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
    x-form
    (map (partial ->api model))]))

(defn xform [query {:keys [before after]}]
  (cond-> query
    before (update 1 comp before)
    after (update 2 comp after)))

(defn insert-many [query entity model]
  (update query :values (comp (partial map (comp
                                             (prep/prepare repos/->sql-value (:table entity))
                                             #(select-keys (->db model %) (:fields entity))))
                              colls/force-sequential)))

(defn modify [query entity model]
  (update query :set (comp (prep/prepare repos/->sql-value (:table entity))
                           #(select-keys (->db model %) (:fields entity)))))
