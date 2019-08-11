(ns com.ben-allred.letshang.api.services.db.repositories.core
  (:require
    [clojure.core.async :as async]
    [clojure.string :as string]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [honeysql.core :as sql]
    [jdbc.pool.c3p0 :as c3p0]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as result-set]
    honeysql-postgres.format
    honeysql-postgres.helpers)
  (:import
    (java.sql ResultSet)))

(defn ^:private sql-value* [table column _]
  [table column])

(defmulti ->api (comp first vector))
(defmulti ->db (comp first vector))
(defmulti ->sql-value #'sql-value*)

(defmethod ->api :default [_ value] value)
(defmethod ->db :default [_ value] value)
(defmethod ->sql-value :default [_ _ value] value)

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

(defonce db-spec
  (c3p0/make-datasource-spec db-cfg))

(defn ^:private sql-format [query]
  (sql/format query :quoting :ansi))

(defn ^:private sql-log [[statement & args]]
  (async/go
    (when (env/get :dev?)
      (let [bindings (volatile! args)]
        (log/info
          (string/replace statement
                          #"(\(| )\?"
                          (fn [[_ prefix]]
                            (let [result (format "%s'%s'" prefix (first @bindings))]
                              (vswap! bindings rest)
                              result))))))))

(defn builder-fn [xform]
  (fn [^ResultSet rs _opts]
    (let [meta (.getMetaData rs)
          collect! (xform conj!)
          cols (mapv (fn [^Integer i] (keyword (.getColumnLabel meta i)))
                     (range 1 (inc (.getColumnCount meta))))
          col-count (count cols)]
      (reify
        result-set/RowBuilder
        (->row [_] (transient {}))
        (column-count [_] col-count)
        (with-column [_ row i]
          (assoc! row
                  (nth cols (dec i))
                  (result-set/read-column-by-index (.getObject rs ^Integer i) meta i)))
        (row! [_ row] (persistent! row))
        result-set/ResultSetBuilder
        (->rs [_] (transient []))
        (with-row [_ mrs row]
          (collect! mrs row))
        (rs! [_ mrs] (persistent! mrs))))))

(defn ^:private exec* [db query xform]
  (let [formatted (sql-format query)]
    (sql-log formatted)
    (jdbc/execute! db formatted {:builder-fn (builder-fn xform)})))

(def ^:private remove-namespaces
  (map (fn remove* [val]
         (cond
           (map? val) (maps/map-kv (comp keyword name) remove* val)
           (coll? val) (map remove* val)
           :else val))))

(defn exec-raw!
  ([db sql]
   (exec-raw! db sql nil))
  ([db sql opts]
   (jdbc/execute! db [sql] opts)))

(defn exec! [query db]
  (let [[query' xform-before xform-after] (colls/force-sequential query)
        xform (cond-> remove-namespaces
                xform-before (->> (comp xform-before))
                xform-after (comp xform-after))]
    (exec* db query' xform)))

(defn transact [f]
  (jdbc/transact (:datasource db-spec) f {:isolation :read-uncommitted}))
