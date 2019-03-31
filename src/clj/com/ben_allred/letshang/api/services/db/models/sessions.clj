(ns com.ben-allred.letshang.api.services.db.models.sessions
  (:require
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.sessions :as repo.sessions]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.common.utils.colls :as colls]))

(defn ^:private select* [db clause]
  (-> clause
      (repo.sessions/select-by)
      (models/select ::repo.sessions/model)
      (repos/exec! db)))

(defn upsert [db user-id]
  (-> (or (-> user-id
              (repo.sessions/upsert)
              (models/insert-many entities/sessions ::repo.sessions/model)
              (repos/exec! db))
          (->> user-id
               (repo.sessions/user-id-clause)
               (select* db)))
      (colls/only!)))

(defn exists? [db id user-id]
  (-> (and id
           user-id
           (-> id
               (repo.sessions/id-clause)
               (repo.sessions/user-id-clause user-id)
               (->> (select* db))
               (colls/only!)))
      (boolean)))
