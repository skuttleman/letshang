(ns com.ben-allred.letshang.api.services.db.models.hangouts
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.hangouts :as repo.hangouts]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(def ^:private model
  (reify models/Model
    (->api [_ hangout]
      hangout)
    (->db [_ hangout]
      (dissoc hangout :created-at :created-by))))

(defn select-for-user [user-id]
  (-> [:= :created-by user-id]
      (repo.hangouts/select-by*)
      (entities/inner-join entities/users :creator [:= :creator.id :hangouts.created-by])
      (models/select model (models/under :hangouts))
      (repos/exec!)))
