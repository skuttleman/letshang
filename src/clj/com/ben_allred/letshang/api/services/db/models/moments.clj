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
  (when (seq hangouts)
    (let [hangout-id->moments (->> [:in :hangout-id (map :id hangouts)]
                                   (select* db)
                                   (group-by :hangout-id))]
      (map (fn [{:keys [id] :as hangout}]
             (assoc hangout :moments (hangout-id->moments id)))
           hangouts))))

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
    (let [new-moment (-> moment
                         (assoc :created-by created-by :hangout-id hangout-id)
                         (repo.moments/insert)
                         (models/insert-many entities/moments ::model)
                         (repos/exec! db)
                         (colls/only!)
                         (->> (models/->api ::model)))]
      (->> {:moment-id (:id new-moment) :user-id created-by :response :positive}
           (models.moment-responses/respond db)
           (vector)
           (assoc new-moment :responses)))))
