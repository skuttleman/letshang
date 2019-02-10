(ns com.ben-allred.letshang.api.services.db.repositories.core
  (:require
    [clojure.java.jdbc :as jdbc]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [honeysql.core :as sql]
    [jdbc.pool.c3p0 :as c3p0]
    [clojure.string :as string]))

(def db-cfg
  {:classname   "org.postgresql.Driver"
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

(defn ^:private sql-log [[statement & args :as query]]
  (when (env/get :dev?)
    (let [bindings (volatile! args)]
      (log/info (string/replace statement #"(\(| )\?" (fn [[_ prefix]]
                                                        (let [result (format "%s'%s'" prefix (first @bindings))]
                                                          (vswap! bindings rest)
                                                          result))))))
  query)

(defn ^:private exec* [db query]
  (let [sql (sql-log (sql-format query))]
    (cond
      (:select query) (jdbc/query db sql)
      (:insert-into query) (jdbc/execute! db sql {:return-keys true})
      :else (log/spy sql))))

(defn ^:private remove-namespaces [val]
  (cond
    (map? val) (->> val
                    (map (fn [[k v]] [(keyword (string/replace (name k) #"_" "-")) (remove-namespaces v)]))
                    (into {}))
    (coll? val) (map remove-namespaces val)
    :else val))

(defn collapse [query-fn & query-fns]
  (reduce (fn [[queries] [queries' f']]
            [(concat queries queries') f'])
          query-fn
          query-fns))

(defn to-sqls [[queries]]
  (map sql-format queries))

(defn exec! [[queries f]]
  (jdbc/db-transaction*
    db-spec
    (fn [db]
      (when (seq queries)
        (loop [[query & more] queries]
          (if (seq more)
            (do (exec* db query)
                (recur more))
            (->> (exec* db query)
                 (sequence f)
                 (remove-namespaces))))))))

(defn single [[query f]]
  [[query] f])

(defn simple [[queries]]
  [queries identity])

(def single-simple (comp single simple vector))
