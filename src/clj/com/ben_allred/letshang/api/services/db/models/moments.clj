(ns com.ben-allred.letshang.api.services.db.models.moments
  (:require
    [clojure.set :as set]
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.moment-responses :as models.moment-responses]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.preparations :as prep]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.invitations :as repo.invitations]
    [com.ben-allred.letshang.api.services.db.repositories.moments :as repo.moments]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]))

(defmethod models/->db ::model
  [_ moment]
  (-> moment
      (dissoc :created-at)
      (set/rename-keys {:window :moment-window})))

(defmethod models/->api ::model
  [_ moment]
  (-> moment
      (set/rename-keys {:moment-window :window})
      (maps/update-maybe :window keyword)))

(def ^:private prepare-window (prep/for-type :moments-window))

(defmethod repos/->sql-value [:moments :moment-window]
  [_ _ value]
  (prepare-window value))

(defn ^:private select* [db clause]
  (-> clause
      (repo.moments/select-by)
      (models/select ::model)
      (repos/exec! db)))

(defn with-moments [db hangouts]
  (let [hangout-id->moments (-> hangouts
                                (some->>
                                  (seq)
                                  (map :id)
                                  (conj [:in :hangout-id])
                                  (select* db))
                                (->> (group-by :hangout-id)))]
    (->> hangouts
         (map (fn [{:keys [id] :as hangout}]
                (assoc hangout :moments (hangout-id->moments id []))))
         (models.moment-responses/with-moment-responses db))))

(defn suggest-moment [db hangout-id moment created-by]
  (when (-> [:and
             [:= :invitations.hangout-id hangout-id]
             [:or
              [:= :invitations.user-id created-by]
              [:= :hangouts.created-by created-by]]]
            (repo.invitations/select-by)
            (entities/inner-join entities/hangouts [:= :hangouts.id :invitations.hangout-id])
            (repos/exec! db)
            (seq))
    (-> moment
        (assoc :created-by created-by :hangout-id hangout-id)
        (repo.moments/insert)
        (models/insert-many entities/moments ::model)
        (assoc :on-conflict [:hangout-id :date :moment-window] :do-nothing [])
        (repos/exec! db)
        (colls/only!))
    (let [moment-id (->> [:and
                          [:= :moments.hangout-id hangout-id]
                          [:= :moments.date (:date moment)]
                          [:= :moments.moment-window (prepare-window (:window moment))]]
                         (select* db)
                         (colls/only!)
                         (:id))]
      (models.moment-responses/respond db {:moment-id moment-id
                                           :user-id   created-by
                                           :response  :positive})
      (->> [{:id hangout-id}]
           (with-moments db)
           (first)
           (:moments)
           (colls/find (comp #{moment-id} :id))))))

(defn set-response [db moment-id response user-id]
  (when (-> [:and
             [:= :moments.id moment-id]
             [:or
              [:= :invitations.user-id user-id]
              [:= :hangouts.created-by user-id]]]
            (repo.invitations/select-by)
            (entities/inner-join entities/hangouts [:= :hangouts.id :invitations.hangout-id])
            (entities/inner-join entities/moments [:= :moments.hangout-id :hangouts.id])
            (repos/exec! db)
            (seq))
    (models.moment-responses/respond db {:moment-id moment-id
                                         :user-id   user-id
                                         :response  response})))
