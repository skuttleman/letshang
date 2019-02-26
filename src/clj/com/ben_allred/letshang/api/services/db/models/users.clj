(ns com.ben-allred.letshang.api.services.db.models.users
  (:require
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.users :as repo.users]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defmethod models/->db ::model
  [_ user]
  (dissoc user :created-at))

(defn ^:private select-by [db clause]
  (-> clause
      (repo.users/select-by)
      (models/select ::model)
      (repos/exec! db)))

(defn ^:private find-by [db clause]
  (->> clause
       (select-by db)
       (colls/only!)))

(defn find-by-email [db email]
  (find-by db [:= :email email]))

(defn select-conflicts [db {:keys [email handle mobile-number]}]
  (-> [:or
       [:= :handle handle]
       [:= :email email]
       [:= :mobile-number mobile-number]]
      (repo.users/select-by)
      (assoc :limit 1)
      (models/select ::model)
      (repos/exec! db)
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
