(ns com.ben-allred.letshang.api.services.db.models.invitations
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.preparations :as prep]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.invitations :as repo.invitations]
    [com.ben-allred.letshang.api.services.db.repositories.users :as repo.users]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.fns :refer [=> =>>]]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]))

(defmethod models/->api ::model
  [_ invitation]
  (-> invitation
      (maps/update-maybe :response keyword)
      (maps/update-maybe :match-type keyword)))

(defmethod repos/->sql-value [:invitations :match-type]
  [_ _ value]
  (prep/invitations-match-type value))

(defmethod repos/->sql-value [:invitations :response]
  [_ _ value]
  (prep/user-response value))

(defn ^:private select* [db clause]
  (-> clause
      (repo.users/select-by)
      (entities/inner-join entities/invitations
                           :invitations
                           [:= :invitations.user-id :users.id]
                           {:id :invitation-id})
      (models/select ::model)
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
          (models/insert-many entities/invitations ::model)
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
        (models/modify entities/invitations ::model)
        (repos/exec! db))))
