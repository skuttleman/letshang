(ns com.ben-allred.letshang.api.services.db.models.invitations
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.preparations :as prep]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.invitations :as repo.invitations]
    [com.ben-allred.letshang.api.services.db.repositories.users :as repo.users]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]))

(defmethod models/->db ::model
  [_ invitation]
  (dissoc invitation :created-at))

(defmethod models/->api ::model
  [_ invitation]
  (-> invitation
      (maps/update-maybe :response keyword)
      (maps/update-maybe :match-type keyword)))

(def ^:private prepare-match-type (prep/for-type :invitations-match-type))
(def ^:private prepare-response (prep/for-type :user-response))

(defmethod repos/->sql-value [:invitations :match-type]
  [_ _ value]
  (prepare-match-type value))

(defmethod repos/->sql-value [:invitations :response]
  [_ _ value]
  (prepare-response value))

(defn ^:private select* [db clause]
  (-> clause
      (repo.users/select-by)
      (update :select conj
              :invitations.hangout-id
              :invitations.response
              :invitations.match-type
              [:invitations.id :invitation-id])
      (update :join (fnil conj []) (:table entities/invitations) [:= :invitations.user-id :users.id])
      (models/select ::model)
      (repos/exec! db)))

(defn with-invitations [db hangouts]
  (if (seq hangouts)
    (let [hangout-id->invitations (->> [:in :hangout-id (map :id hangouts)]
                                    (select* db)
                                    (group-by :hangout-id))]
      (map (fn [{:keys [id] :as hangout}]
             (assoc hangout :invitations (hangout-id->invitations id)))
           hangouts))
    hangouts))

(defn insert-many! [db hangout-ids user-ids created-by]
  (let [hangout-invitations (for [hangout-id hangout-ids
                               user-id user-ids]
                           {:hangout-id hangout-id :match-type :exact :user-id user-id :created-by created-by})]
    (when (seq hangout-invitations)
      (-> hangout-invitations
          (->> (entities/insert-into entities/invitations))
          (models/insert-many entities/invitations ::model)
          (repos/exec! db)))))

(defn set-response [db invitation-id response user-id]
  (when (-> [:and
             [:= :invitations.id invitation-id]
             [:= :invitations.user-id user-id]]
            (repo.invitations/select-by)
            (repos/exec! db)
            (colls/only!))
    (-> {:response response}
        (repo.invitations/modify [:= :invitations.id invitation-id])
        (models/modify entities/invitations ::model)
        (repos/exec! db))))
