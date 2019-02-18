(ns com.ben-allred.letshang.api.services.db.repositories.core
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as string]
    [com.ben-allred.letshang.api.services.db.preparations :as prep]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [honeysql.core :as sql]
    [jdbc.pool.c3p0 :as c3p0]))

(defn ^:private sql-value* [table column _]
  [table column])

(defmulti ->sql-value #'sql-value*)
(defmethod ->sql-value :default
  [_ _ value]
  value)

(def db-cfg
  {:vendor      "postgres"
   :classname   "org.postgresql.Driver"
   :subprotocol "postgresql"
   :user        (env/get :db-user)
   :password    (env/get :db-password)
   :subname     (format "//%s:%s/%s"
                        (env/get :db-host)
                        (env/get :db-port)
                        (env/get :db-name))})

(def ^:private db-spec
  (c3p0/make-datasource-spec
    db-cfg))

(defn ^:private sql-format [query]
  (sql/format query :quoting :ansi))

(defn ^:private sql-log [query]
  (when (env/get :dev?)
    (let [[statement & args] (sql-format query)
          bindings (volatile! args)]
      (log/info
        (string/replace statement
                        #"(\(| )\?"
                        (fn [[_ prefix]]
                          (let [result (format "%s'%s'" prefix (first @bindings))]
                            (vswap! bindings rest)
                            result)))))))

(defn ^:private exec* [db query]
  (let [{insert-table :insert-into update-table :update} query]
    (sql-log query)
    (cond
      (:select query) (jdbc/query db (sql-format query))
      update-table (->> (keywords/kebab->snake update-table)
                        (prep/prepare ->sql-value)
                        (update query :set)
                        (sql-format)
                        (jdbc/execute! db))
      insert-table (->> (:values query)
                        (colls/force-sequential)
                        (map (prep/prepare ->sql-value (keywords/kebab->snake insert-table)))
                        (jdbc/insert-multi! db (keywords/kebab->snake insert-table)))
      :else query)))

(defn ^:private remove-namespaces [val]
  (cond
    (map? val) (maps/map-kv keywords/snake->kebab remove-namespaces val)
    (coll? val) (map remove-namespaces val)
    :else val))

(defn exec! [queries db]
  (let [[query xform-before xform-after] (colls/force-sequential queries)]
    (-> (exec* db query)
        (cond->> xform-before (sequence xform-before))
        (remove-namespaces)
        (cond->> xform-after (sequence xform-after)))))

(defn transact [f]
  (jdbc/db-transaction* db-spec f {:isolation :read-uncommitted}))
