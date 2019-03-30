(ns com.ben-allred.letshang.integration.utils.fixtures
  (:require
    [com.ben-allred.letshang.api.server :as server]
    [com.ben-allred.letshang.api.services.db.migrations :as migrations]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.integration.utils.helpers :as h]
    [immutant.web :as web]
    [jdbc.pool.c3p0 :as c3p0])
  (:import
    (java.io Closeable)))

(defn with-db [test-fn]
  (repos/exec-raw! repos/db-spec (format "DROP DATABASE IF EXISTS %s" "lets_hang_test") {:transaction? false})
  (let [current-db-name (env/get :db-name)]
    (alter-var-root #'env/get assoc :db-name "lets_hang_test")
    (repos/exec-raw! repos/db-spec (format "CREATE DATABASE %s" "lets_hang_test") {:transaction? false})
    (with-redefs [repos/db-spec (c3p0/make-datasource-spec (repos/db-cfg))]
      (try
        (migrations/migrate!)
        (test-fn)
        (finally
          (.close ^Closeable repos/db-spec)
          (alter-var-root #'env/get assoc :db-name current-db-name))))))

(defn with-seed
  ([]
   (with-seed "default.sql"))
  ([seed-file]
   (fn [test-fn]
     (repos/transact
       (fn [db]
         (doseq [table ["invitations" "moment_responses" "location_responses" "moments" "locations" "hangouts" "users"]]
           (repos/exec-raw! db (format "TRUNCATE %s CASCADE" table)))))
     (h/seed! seed-file)
     (test-fn))))

(defn with-app
  ([]
   (with-app 4000))
  ([port]
   (fn [test-fn]
     (with-redefs [env/get (assoc env/get :base-url (str "http://localhost:" port))]
       (let [server (server/-main "PORT" port)]
         (test-fn)
         (web/stop server))))))
