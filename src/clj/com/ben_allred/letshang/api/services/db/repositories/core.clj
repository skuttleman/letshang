(ns com.ben-allred.letshang.api.services.db.repositories.core
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as string]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [honeysql.core :as sql]
    [jdbc.pool.c3p0 :as c3p0])
  (:import (clojure.lang Keyword)
           (org.postgresql.util PGobject)))

(declare transact)

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
      (log/info (string/replace statement #"(\(| )\?" (fn [[_ prefix]]
                                                        (let [result (format "%s'%s'" prefix (first @bindings))]
                                                          (vswap! bindings rest)
                                                          result)))))))
(extend-protocol jdbc/ISQLValue
  Keyword
  (sql-value [val]
    (doto (PGobject.)
      (.setType "invitees_match_type")
      (.setValue (name val)))))

(defn ^:private exec* [db query]
  (sql-log query)
  (cond
    (:select query) (jdbc/query db (sql-format query))
    (:insert-into query) (->> (:values query)
                              (colls/force-sequential)
                              (map (partial maps/map-keys #(keywords/replace % #"-" :_)))
                              (jdbc/insert-multi! db (keywords/replace (:insert-into query) #"-" :_)))
    :else query))

(defn ^:private remove-namespaces [val]
  (cond
    (map? val) (->> val
                    (map (fn [[k v]] [(keyword (string/replace (name k) #"_" "-")) (remove-namespaces v)]))
                    (into {}))
    (coll? val) (map remove-namespaces val)
    :else val))

(defn exec! [queries db]
  (if db
    (let [[query & query-fs] (colls/force-sequential queries)]
      (->> query-fs
           (reduce (fn [result query-f]
                     (conj result (query-f result)))
                   [(exec* db query)])
           peek
           (remove-namespaces)))
    (transact (constantly queries))))

(defn transact [f]
  (jdbc/db-transaction* db-spec #(exec! (f %) %) {:isolation :read-uncommitted}))
