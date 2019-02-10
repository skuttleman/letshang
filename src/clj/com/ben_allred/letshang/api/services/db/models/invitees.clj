(ns com.ben-allred.letshang.api.services.db.models.invitees
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.users :as repo.users]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(def ^:private model
  (reify models/Model
    (->api [_ hangout]
      hangout)
    (->db [_ hangout]
      (dissoc hangout :created-at :created-by))))

(defn ^:private select* [clause]
  (-> clause
      (repo.users/select-by*)
      (update :select conj :invitees.hangout-id)
      (update :join (fnil conj []) (:table entities/invitees) [:= :invitees.user-id :users.id])
      (models/select model)
      (repos/exec!)))

(defn with-invitees [hangouts]
  (if (seq hangouts)
    (let [hangout-id->invitees (->> [:in :hangout-id (map :id hangouts)]
                                    (select*)
                                    (group-by :hangout-id))]
      (map (fn [{:keys [id] :as hangout}]
             (assoc hangout :invitees (hangout-id->invitees id)))
           hangouts))
    hangouts))
