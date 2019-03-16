(ns com.ben-allred.letshang.api.services.db.repositories.core
  (:require
    [clojure.core.async :as async]
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as string]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [honeysql.core :as sql]
    [jdbc.pool.c3p0 :as c3p0]
    honeysql-postgres.format
    honeysql-postgres.helpers)
  (:import
    (java.util Date)))

(defn ^:private sql-value* [table column _]
  [table column])

(defmulti ->sql-value #'sql-value*)
(defmethod ->sql-value :default
  [_ _ value]
  value)

(extend-protocol jdbc/ISQLValue
  Date
  (sql-value [val]
    (java.sql.Date. (.getTime val))))

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
  (async/go
    (when (env/get :dev?)
      (let [[statement & args] (sql-format query)
            bindings (volatile! args)]
        (log/info
          (string/replace statement
                          #"(\(| )\?"
                          (fn [[_ prefix]]
                            (let [result (format "%s'%s'" prefix (first @bindings))]
                              (vswap! bindings rest)
                              result))))))))

(defn ^:private exec* [db query]
  (let [{insert-table :insert-into update-table :update} query]
    (sql-log query)
    (cond
      (:select query) (jdbc/query db (sql-format query))
      update-table (jdbc/execute! db (sql-format query))
      insert-table (->> (when-let [returning (:returning query)]
                          {:return-keys (map name returning)})
                        (jdbc/execute! db (sql-format (dissoc query :returning)))
                        (colls/force-sequential))
      :else query)))

(defn ^:private remove-namespaces [val]
  (cond
    (map? val) (maps/map-kv keywords/snake->kebab remove-namespaces val)
    (coll? val) (map remove-namespaces val)
    :else val))

(defn exec-raw! [db sql]
  (jdbc/execute! db [sql]))

(defn exec! [query db]
  (let [[query' xform-before xform-after] (colls/force-sequential query)]
    (-> (exec* db query')
        (cond->> xform-before (sequence xform-before))
        (remove-namespaces)
        (cond->> xform-after (sequence xform-after)))))

(defn transact [f]
  (jdbc/db-transaction* db-spec f {:isolation :read-uncommitted}))
