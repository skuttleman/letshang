(ns com.ben-allred.letshang.api.services.db.models.users
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.users :as repo.users]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn ^:private select-by [db clause]
  (-> clause
      (repo.users/select-by)
      (models/select ::repo.users/model)
      (repos/exec! db)))

(defn ^:private find-by [db clause]
  (->> clause
       (select-by db)
       (colls/only!)))

(defn find-by-email [db email]
  (find-by db (repo.users/email-clause email)))

(defn select-conflicts [db user]
  (-> user
      (repo.users/conflict-clause)
      (repo.users/select-by)
      (entities/limit 1)
      (models/select ::repo.users/model)
      (repos/exec! db)
      (colls/only!)))

(defn find-known-associates [db user-id]
  (-> user-id
      (repo.users/select-known-associates)
      (models/select ::repo.users/model)
      (repos/exec! db)))

(defn create [db user]
  (when (-> user
            (repo.users/insert)
            (repos/exec! db))
    (find-by-email db (:email user))))
