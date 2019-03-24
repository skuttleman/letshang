(ns com.ben-allred.letshang.api.services.db.models.moment-responses
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.moment-responses :as repo.moment-responses]
    [com.ben-allred.letshang.common.utils.fns :refer [=> =>>]]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn ^:private select* [db clause]
  (-> clause
      (repo.moment-responses/select-by)
      (models/select ::repo.moment-responses/model)
      (repos/exec! db)))

(defn with-moment-responses [db moments]
  (models/with :responses
               (=>> (repo.moment-responses/moment-ids-clause)
                    (select* db))
               [:id :moment-id]
               moments))

(defn respond [db moment-response]
  (-> moment-response
      (repo.moment-responses/upsert)
      (models/insert-many entities/moment-responses ::repo.moment-responses/model)
      (repos/exec! db))
  moment-response)
