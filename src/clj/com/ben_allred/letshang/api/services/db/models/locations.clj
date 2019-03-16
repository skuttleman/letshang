(ns com.ben-allred.letshang.api.services.db.models.locations
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.location-responses :as models.location-responses]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.invitations :as repo.invitations]
    [com.ben-allred.letshang.api.services.db.repositories.locations :as repo.locations]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.fns :refer [=>>]]))

(defmethod models/->db ::model
  [_ location]
  (-> location
      (dissoc :created-at)))

(defmethod models/->api ::model
  [_ location]
  location)

(defn ^:private select* [db clause]
  (-> clause
      (repo.locations/select-by)
      (models/select ::model)
      (repos/exec! db)))

(defn with-locations [db hangouts]
  (->> hangouts
       (models/with :locations
                    (=>> (repo.locations/hangout-ids-clause)
                         (select* db))
                    [:id :hangout-id])
       (models.location-responses/with-location-responses db)))

(defn suggest-location [db hangout-id location created-by]
  (when (-> created-by
            (repo.invitations/has-hangout-clause)
            (repo.invitations/hangout-id-clause hangout-id)
            (repo.invitations/select-by)
            (entities/inner-join entities/hangouts [:= :hangouts.id :invitations.hangout-id])
            (repos/exec! db)
            (seq))
    (let [location-id (-> location
                          (assoc :created-by created-by :hangout-id hangout-id)
                          (repo.locations/insert)
                          (models/insert-many entities/locations ::model)
                          (repos/exec! db)
                          (colls/only!)
                          (:id))]
      (models.location-responses/respond db {:location-id location-id
                                             :user-id     created-by
                                             :response    :positive})
      (->> [{:id hangout-id}]
           (with-locations db)
           (first)
           (:locations)
           (colls/find (comp #{location-id} :id))))))

(defn set-response [db location-id response user-id]
  (when (-> location-id
            (repo.locations/id-clause)
            (repo.invitations/has-hangout-clause user-id)
            (repo.invitations/select-by)
            (entities/inner-join entities/hangouts [:= :hangouts.id :invitations.hangout-id])
            (entities/inner-join entities/locations [:= :locations.hangout-id :hangouts.id])
            (repos/exec! db)
            (seq))
    (models.location-responses/respond db {:location-id location-id
                                           :user-id     user-id
                                           :response    response})))
