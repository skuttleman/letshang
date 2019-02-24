(ns com.ben-allred.letshang.api.services.db.models.users
  (:require
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.users :as repo.users]
    [com.ben-allred.letshang.common.utils.colls :as colls]))

(defmethod models/->db ::model
  [_ user]
  (dissoc user :created-at))

(defn select-by-email [db email]
  (-> [:= :email email]
      (repo.users/select-by)
      (models/select ::model)
      (repos/exec! db)))

(defn find-by-email [db email]
  (->> email
       (select-by-email db)
       (colls/only!)))

(defn find-known-associates [db user-id]
  (-> user-id
      (repo.users/select-known-associates)
      (models/select ::model)
      (repos/exec! db)))

(defn create [db user]
  (when (-> user
            (repo.users/insert)
            (repos/exec! db))
    (find-by-email db (:email user))))
