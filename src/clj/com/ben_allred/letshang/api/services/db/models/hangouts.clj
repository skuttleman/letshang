(ns com.ben-allred.letshang.api.services.db.models.hangouts
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.invitees :as models.invitees]
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

(defn ^:private has-hangout [user-id]
  [:or
   [:= :hangouts.created-by user-id]
   [:exists {:select [:id]
             :from   [:invitees]
             :where  [:and
                      [:= :invitees.hangout-id :hangouts.id]
                      [:= :invitees.user-id user-id]]}]])

(defn ^:private select* [db clause]
  (-> clause
      (repo.hangouts/select-by*)
      (entities/inner-join entities/users :creator [:= :creator.id :hangouts.created-by])
      (models/select ::model (models/under :hangouts))
      (repos/exec! db)))

(defn select-for-user [db user-id]
  (-> user-id
      (has-hangout)
      (->> (select* db))))

(defn find-for-user [db hangout-id user-id]
  (->> [:and
        [:= :hangouts.id hangout-id]
        (has-hangout user-id)]
       (select* db)
       (models.invitees/with-invitees db)
       (colls/only!)))

(defn create [db hangout created-by]
  (let [hangout-id (-> hangout
                       (assoc :created-by created-by)
                       (repo.hangouts/insert)
                       (repos/exec! db)
                       (colls/only!)
                       (:id))]
    (models.invitees/insert-many! db [hangout-id] (:invitee-ids hangout) created-by)
    (find-for-user db hangout-id created-by)))

(defn modify [db hangout-id hangout created-by]
  (when (-> [:and
             [:= :hangouts.id hangout-id]
             [:= :hangouts.created-by created-by]]
            (repo.hangouts/select-by*)
            (repos/exec! db)
            (colls/only!))
    (-> hangout
        (repo.hangouts/modify [:= :hangouts.id hangout-id])
        (update :set select-keys #{:name})
        (repos/exec! db))
    (models.invitees/insert-many! db [hangout-id] (:invitee-ids hangout) created-by)
    (find-for-user db hangout-id created-by)))
