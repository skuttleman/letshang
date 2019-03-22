(ns com.ben-allred.letshang.api.services.db.models.invitations
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.hangouts :as repo.hangouts]
    [com.ben-allred.letshang.api.services.db.repositories.invitations :as repo.invitations]
    [com.ben-allred.letshang.api.services.db.repositories.users :as repo.users]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.fns :refer [=> =>>]]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn ^:private select* [db clause]
  (-> clause
      (repo.users/select-by)
      (entities/inner-join entities/invitations
                           :invitations
                           [:= :invitations.user-id :users.id]
                           {:id :invitation-id})
      (models/select ::repo.invitations/model)
      (repos/exec! db)))

(defn with-invitations [db hangouts]
  (models/with :invitations
               (=>> (repo.invitations/hangout-ids-clause)
                    (select* db))
               [:id :hangout-id]
               hangouts))

(defn insert-many! [db hangout-ids user-ids created-by]
  (some-> (for [hangout-id hangout-ids
                user-id user-ids]
            {:hangout-id hangout-id :match-type :exact :user-id user-id :created-by created-by})
          (seq)
          (->> (entities/insert-into entities/invitations))
          (entities/on-conflict-nothing [:hangout-id :user-id])
          (models/insert-many entities/invitations ::repo.invitations/model)
          (repos/exec! db)))

(defn set-response [db invitation-id response user-id]
  (when (-> user-id
            (repo.invitations/user-clause)
            (repo.invitations/id-clause invitation-id)
            (repo.invitations/select-by)
            (repos/exec! db)
            (colls/only!))
    (-> {:response response}
        (repo.invitations/modify (repo.invitations/id-clause invitation-id))
        (models/modify entities/invitations ::repo.invitations/model)
        (repos/exec! db))))

(defn suggest-invitees [db hangout-id invitation user-id]
  (let [{:keys [created-by others-invite? invitee-id]}
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
    (when (or (= user-id created-by) (and others-invite? (= user-id invitee-id)))
      (insert-many! db [hangout-id] (:invitation-ids invitation) user-id)
      (->> [{:id hangout-id}]
           (with-invitations db)
           (colls/only!)
           (:invitations)
           (filter (comp (set (:invitation-ids invitation)) :user-id))))))
