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

(defn with-moments [db hangouts]
  (->> hangouts
       (models/with :moments
                    (=>> (repo.moments/hangout-ids-clause)
                         (select* db))
                    [:id :hangout-id])
       (models.moment-responses/with-moment-responses db)))

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
                                {:user-id    :invitee-id
                                 :created-by nil})
            (models/select ::repo.hangouts/model)
            (repos/exec! db)
            (colls/only!))]
    (when (or (= created-by user-id) (and when-suggestions? (= invitee-id user-id)))
      (-> moment
          (assoc :created-by user-id :hangout-id hangout-id)
          (repo.moments/insert)
          (models/insert-many entities/moments ::repo.moments/model)
          (entities/on-conflict-nothing [:hangout-id :date :moment-window])
          (repos/exec! db)
          (colls/only!))
      (let [moment-id (-> moment
                          (assoc :hangout-id hangout-id)
                          (prepare-all)
                          (repo.moments/moment-window-clause)
                          (->> (select* db))
                          (colls/only!)
                          (:id))]
        (models.moment-responses/respond db {:moment-id moment-id
                                             :user-id   user-id
                                             :response  :positive})
        (->> [{:id hangout-id}]
             (with-moments db)
             (first)
             (:moments)
             (colls/find (comp #{moment-id} :id)))))))

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
