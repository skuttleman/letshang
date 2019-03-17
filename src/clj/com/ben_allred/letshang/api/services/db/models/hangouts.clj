(ns com.ben-allred.letshang.api.services.db.models.hangouts
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.invitations :as models.invitations]
    [com.ben-allred.letshang.api.services.db.models.locations :as models.locations]
    [com.ben-allred.letshang.api.services.db.models.moments :as models.moments]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.models.users :as models.users]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.hangouts :as repo.hangouts]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn ^:private select* [db clause]
  (-> clause
      (repo.hangouts/select-by)
      (entities/inner-join entities/users :creator [:= :creator.id :hangouts.created-by])
      (models/select ::repo.hangouts/model (models/under :hangouts))
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

(defn create [db hangout user-id]
  (let [hangout-id (-> hangout
                       (assoc :created-by user-id)
                       (repo.hangouts/insert)
                       (models/insert-many entities/hangouts ::repo.hangouts/model)
                       (repos/exec! db)
                       (colls/only!)
                       (:id))]
    (models.invitations/insert-many! db [hangout-id] (:invitation-ids hangout) user-id)
    (find-for-user db hangout-id user-id)))

(defn modify [db hangout-id hangout user-id]
  (let [{:keys [created-by invitee-id others-invite?]}
        (-> hangout-id
            (repo.hangouts/id-clause)
            (repo.hangouts/select-by)
            (entities/left-join entities/invitations
                                :invitations
                                [:and
                                 [:= :invitations.hangout-id :hangouts.id]
                                 [:= :invitations.user-id user-id]]
                                {:user-id    :invitee-id
                                 :created-by nil})
            (models/select ::repo.hangouts/model)
            (repos/exec! db)
            (colls/only!))]
    (when (= user-id created-by)
      (-> hangout
          (repo.hangouts/modify (repo.hangouts/id-clause hangout-id))
          (models/modify entities/hangouts ::repo.hangouts/model)
          (update :set dissoc :invitation-ids)
          (repos/exec! db)))
    (when (or (= user-id created-by) (and others-invite? (= user-id invitee-id)))
      (models.invitations/insert-many! db [hangout-id] (:invitation-ids hangout) user-id)
      (find-for-user db hangout-id user-id))))
