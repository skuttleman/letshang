(ns com.ben-allred.letshang.api.services.db.models.moments
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.moment-responses :as models.moment-responses]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.preparations :as prep]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.hangouts :as repo.hangouts]
    [com.ben-allred.letshang.api.services.db.repositories.invitations :as repo.invitations]
    [com.ben-allred.letshang.api.services.db.repositories.moments :as repo.moments]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.fns :refer [=> =>>]]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]))

(defn ^:private prepare-all [moment]
  (-> moment
      (->> (repos/->db ::repo.moments/model))
      (maps/update-maybe :moment-window prep/moments-window)
      (maps/update-maybe :date prep/date)))

(defn ^:private select* [db clause]
  (-> clause
      (repo.moments/select-by)
      (models/select ::repo.moments/model)
      (repos/exec! db)))

(defn ^:private can-moment [clause created-by]
  (-> clause
      (repo.invitations/has-hangout-clause created-by)
      (repo.invitations/select-by)
      (entities/inner-join entities/hangouts [:= :hangouts.id :invitations.hangout-id])))

(defn select-for-hangout [db hangout-id user-id]
  (let [{:keys [created-by invitee-id]}
        (-> hangout-id
            (repo.hangouts/id-clause)
            (repo.hangouts/select-by)
            (entities/left-join entities/invitations
                                :invitations
                                [:and
                                 [:= :invitations.hangout-id :hangouts.id]
                                 [:= :invitations.user-id user-id]]
                                {:user-id    :invitee/invitee-id
                                 :created-by nil})
            (models/select ::repo.hangouts/model)
            (repos/exec! db)
            (colls/only!))]
    (when (or (= created-by user-id) (= invitee-id user-id))
      (->> hangout-id
           (repo.moments/hangout-id-clause)
           (select* db)
           (models.moment-responses/with-moment-responses db)))))

(defn suggest-moment [db hangout-id moment user-id]
  (let [{:keys [created-by invitee-id when-suggestions?]}
        (-> hangout-id
            (repo.hangouts/id-clause)
            (repo.hangouts/select-by)
            (entities/left-join entities/invitations
                                :invitations
                                [:and
                                 [:= :invitations.hangout-id :hangouts.id]
                                 [:= :invitations.user-id user-id]]
                                {:user-id    :invitee/invitee-id
                                 :created-by nil})
            (models/select ::repo.hangouts/model)
            (repos/exec! db)
            (colls/only!))]
    (when (or (= created-by user-id) (and when-suggestions? (= invitee-id user-id)))
      (let [moment-id (-> moment
                          (assoc :created-by user-id :hangout-id hangout-id)
                          (repo.moments/insert)
                          (models/insert-many entities/moments ::repo.moments/model)
                          (entities/on-conflict-nothing [:hangout-id :date :moment-window])
                          (repos/exec! db)
                          (colls/only!)
                          (:id))]
        (models.moment-responses/respond db {:moment-id moment-id
                                             :user-id   user-id
                                             :response  :positive})
        (->> hangout-id
             (repo.moments/hangout-id-clause)
             (select* db)
             (models.moment-responses/with-moment-responses db)
             (colls/find (comp #{moment-id} :id)))))))

(defn lock-moment [db moment-id patch user-id]
  (when (-> moment-id
            (repo.moments/id-clause)
            (repo.hangouts/creator-clause user-id)
            (repo.moments/select-by)
            (entities/inner-join entities/hangouts [:= :hangouts.id :moments.hangout-id])
            (repos/exec! db)
            (seq))
    (-> moment-id
        (repo.moments/id-clause)
        (->> (repo.moments/modify (select-keys patch #{:locked? :moment-time})))
        (models/modify entities/moments ::repo.moments/model)
        (repos/exec! db))
    (->> moment-id
         (repo.moments/id-clause)
         (select* db)
         (models.moment-responses/with-moment-responses db)
         (colls/only!))))

(defn set-response [db moment-id response user-id]
  (when (-> moment-id
            (repo.moments/id-clause)
            (can-moment user-id)
            (entities/inner-join entities/moments [:= :moments.hangout-id :hangouts.id])
            (repos/exec! db)
            (seq))
    (models.moment-responses/respond db {:moment-id moment-id
                                         :user-id   user-id
                                         :response  response})))
