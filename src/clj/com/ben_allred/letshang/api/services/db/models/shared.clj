(ns com.ben-allred.letshang.api.services.db.models.shared
  (:require
    [com.ben-allred.letshang.api.services.db.preparations :as prep]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn ^:private with* [k f [pk fk] values]
  (let [pk->results (-> values
                        (seq)
                        (some->>
                          (map #(get % pk))
                          (f))
                        (->> (group-by fk)))]
    (fn [value]
      (assoc value k (pk->results (get value pk) [])))))

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
    (map (partial repos/->api model))]))

(defn xform [query {:keys [before after]}]
  (cond-> query
    before (update 1 comp before)
    after (update 2 comp after)))

(defn insert-many [query entity model]
  (update query :values (comp (partial map (comp
                                             (prep/prepare repos/->sql-value (:table entity))
                                             #(select-keys (repos/->db model %) (disj (:fields entity) :created-at :id))))
                              colls/force-sequential)))

(defn modify [query entity model]
  (update query :set (comp (prep/prepare repos/->sql-value (:table entity))
                           #(select-keys (repos/->db model %) (disj (:fields entity) :created-at :created-by :id)))))

(defn with [k f [pk fk] values]
  (map (with* k f [pk fk] values) values))

(defn with-inner [outer-k k f [pk fk] values]
  (colls/supdate values map update outer-k colls/supdate map (with* k f [pk fk] values)))
