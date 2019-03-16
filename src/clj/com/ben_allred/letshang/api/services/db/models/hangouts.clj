(ns com.ben-allred.letshang.api.services.db.models.hangouts
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.invitations :as models.invitations]
    [com.ben-allred.letshang.api.services.db.models.locations :as models.locations]
    [com.ben-allred.letshang.api.services.db.models.moments :as models.moments]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.hangouts :as repo.hangouts]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defmethod models/->api ::model
  [_ hangout]
  hangout)

(defmethod models/->db ::model
  [_ hangout]
  (dissoc hangout :created-at :id))

(defn ^:private select* [db clause]
  (-> clause
      (repo.hangouts/select-by)
      (entities/inner-join entities/users :creator [:= :creator.id :hangouts.created-by])
      (models/select ::model (models/under :hangouts))
      (repos/exec! db)))

(defn select-for-user [db user-id]
  (-> user-id
      (repo.hangouts/has-hangout-clause)
      (->> (select* db))))

(defn find-for-user [db hangout-id user-id]
  (-> hangout-id
      (repo.hangouts/id-clause)
      (repo.hangouts/has-hangout-clause user-id)
      (->>
        (select* db)
        (models.invitations/with-invitations db)
        (models.moments/with-moments db)
        (models.locations/with-locations db)
        (colls/only!))))

(defn create [db hangout created-by]
  (let [hangout-id (-> hangout
                       (assoc :created-by created-by)
                       (repo.hangouts/insert)
                       (models/insert-many entities/hangouts ::model)
                       (repos/exec! db)
                       (colls/only!)
                       (:id))]
    (models.invitations/insert-many! db [hangout-id] (:invitation-ids hangout) created-by)
    (find-for-user db hangout-id created-by)))

(defn modify [db hangout-id hangout created-by]
  (when (-> hangout-id
            (repo.hangouts/id-clause)
            (repo.hangouts/creator-clause created-by)
            (repo.hangouts/select-by)
            (repos/exec! db)
            (colls/only!))
    (-> hangout
        (repo.hangouts/modify (repo.hangouts/creator-clause hangout-id))
        (models/modify entities/hangouts ::model)
        (update :set select-keys #{:name})
        (repos/exec! db))
    (models.invitations/insert-many! db [hangout-id] (:invitation-ids hangout) created-by)
    (find-for-user db hangout-id created-by)))
