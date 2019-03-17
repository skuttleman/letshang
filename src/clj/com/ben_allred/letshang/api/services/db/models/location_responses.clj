(ns com.ben-allred.letshang.api.services.db.models.location-responses
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.location-responses :as repo.location-responses]
    [com.ben-allred.letshang.api.services.db.repositories.locations :as repo.locations]
    [com.ben-allred.letshang.common.utils.fns :refer [=>]]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn with-location-responses [db hangouts]
  (models/with-inner :locations
                     :responses
                     (=> (repo.locations/hangout-ids-clause)
                         (repo.location-responses/select-by)
                         (entities/inner-join entities/locations
                                              [:= :location-responses.location-id :locations.id])
                         (models/select ::repo.location-responses/model)
                         (models/xform {:after (map #(select-keys % (:fields entities/location-responses)))})
                         (repos/exec! db))
                     [:id :location-id]
                     hangouts))

(defn respond [db location-response]
  (-> location-response
      (repo.location-responses/upsert)
      (models/insert-many entities/location-responses ::repo.location-responses/model)
      (repos/exec! db))
  location-response)
