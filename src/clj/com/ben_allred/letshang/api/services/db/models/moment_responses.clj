(ns com.ben-allred.letshang.api.services.db.models.moment-responses
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.moment-responses :as repo.moment-responses]
    [com.ben-allred.letshang.api.services.db.repositories.moments :as repo.moments]
    [com.ben-allred.letshang.common.utils.fns :refer [=>]]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn with-moment-responses [db hangouts]
  (models/with-inner :moments
                     :responses
                     (=> (repo.moments/hangout-ids-clause)
                         (repo.moment-responses/select-by)
                         (entities/inner-join entities/moments
                                              [:= :moment-responses.moment-id :moments.id])
                         (models/select ::repo.moment-responses/model)
                         (models/xform {:after (map #(select-keys % (:fields entities/moment-responses)))})
                         (repos/exec! db))
                     [:id :moment-id]
                     hangouts))

(defn respond [db moment-response]
  (-> moment-response
      (repo.moment-responses/upsert)
      (models/insert-many entities/moment-responses ::repo.moment-responses/model)
      (repos/exec! db))
  moment-response)
