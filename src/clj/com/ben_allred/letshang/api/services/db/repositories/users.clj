(ns com.ben-allred.letshang.api.services.db.repositories.users
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]))

(defn select-by* [clause]
  (-> entities/users
      (entities/select)
      (assoc :where clause)
      (repos/single-simple)
      (repos/exec!)))

(defn find-by* [clause]
  (-> clause
      (select-by*)
      (colls/only!)))
