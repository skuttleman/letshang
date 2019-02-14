(ns com.ben-allred.letshang.api.services.db.models.invitees
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.users :as repo.users]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defmethod models/->db ::model
  [_ hangout]
  (dissoc hangout :created-at))

(defn ^:private select* [db clause]
  (-> clause
      (repo.users/select-by*)
      (update :select conj :invitees.hangout-id)
      (update :join (fnil conj []) (:table entities/invitees) [:= :invitees.user-id :users.id])
      (models/select ::model)
      (repos/exec! db)))

(defn with-invitees
  ([hangouts]
   (with-invitees nil hangouts))
  ([db hangouts]
   (if (seq hangouts)
     (let [hangout-id->invitees (->> [:in :hangout-id (map :id hangouts)]
                                     (select* db)
                                     (group-by :hangout-id))]
       (map (fn [{:keys [id] :as hangout}]
              (assoc hangout :invitees (hangout-id->invitees id)))
            hangouts))
     hangouts)))

(defn insert-many!
  ([hangout-ids user-ids created-by]
   (insert-many! nil hangout-ids user-ids created-by))
  ([db hangout-ids user-ids created-by]
   (when-let [hangout-invitees (seq (for [hangout-id hangout-ids
                                          user-id user-ids]
                                      {:hangout-id hangout-id :match-type :exact :user-id user-id :created-by created-by}))]

     (-> hangout-invitees
         (->> (entities/insert-into entities/invitees))
         (models/insert-many entities/invitees ::model)
         (repos/exec! db)))))
