(ns com.ben-allred.letshang.api.services.db.models.location-responses
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.location-responses :as repo.location-responses]
    [com.ben-allred.letshang.common.utils.fns :refer [=> =>>]]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn ^:private select* [db clause]
  (-> clause
      (repo.location-responses/select-by)
      (models/select ::repo.location-responses/model)
      (repos/exec! db)))

(defn with-location-responses [db locations]
  (models/with :responses
               (=>> (repo.location-responses/location-ids-clause)
                    (select* db))
               [:id :location-id]
               locations))

(defn respond [db location-response]
  (-> location-response
      (repo.location-responses/upsert)
      (models/insert-many entities/location-responses ::repo.location-responses/model)
      (repos/exec! db))
  location-response)
