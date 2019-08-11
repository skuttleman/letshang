(ns com.ben-allred.letshang.api.services.db.models.locations
  (:require
    [clojure.string :as string]
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.location-responses :as models.location-responses]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.hangouts :as repo.hangouts]
    [com.ben-allred.letshang.api.services.db.repositories.invitations :as repo.invitations]
    [com.ben-allred.letshang.api.services.db.repositories.locations :as repo.locations]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.fns :refer [=>>]]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [honeysql.core :as sql]))

(defn ^:private select* [db clause]
  (-> clause
      (repo.locations/select-by)
      (models/select ::repo.locations/model)
      (repos/exec! db)))

(defn ^:private select-by-hangout-id [db hangout-id]
  (->> hangout-id
       (repo.locations/hangout-id-clause)
       (select* db)
       (models.location-responses/with-location-responses db)))

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
      (select-by-hangout-id db hangout-id))))

(defn suggest-location [db hangout-id location user-id]
  (let [location (maps/update-maybe location :name string/trim)
        {:keys [created-by invitee-id where-suggestions?]}
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
    (when (or (= created-by user-id) (and where-suggestions? (= invitee-id user-id)))
      (-> location
          (assoc :created-by user-id :hangout-id hangout-id)
          (repo.locations/insert)
          (models/insert-many entities/locations ::repo.locations/model)
          (entities/on-conflict-nothing [:hangout-id (sql/call :lower :name)])
          (repos/exec! db)
          (colls/only!))
      (let [location-id (-> location
                            (assoc :hangout-id hangout-id)
                            (repo.locations/location-hangout-name-clause)
                            (->> (select* db))
                            (colls/only!)
                            (:id))]
        (models.location-responses/respond db {:location-id location-id
                                               :user-id     user-id
                                               :response    :positive})
        (->> hangout-id
             (select-by-hangout-id db)
             (colls/find (comp #{location-id} :id)))))))

(defn lock-location [db location-id locked? user-id]
  (when (-> location-id
            (repo.locations/id-clause)
            (repo.hangouts/creator-clause user-id)
            (repo.locations/select-by)
            (entities/inner-join entities/hangouts [:= :hangouts.id :locations.hangout-id])
            (repos/exec! db)
            (seq))
    (-> location-id
        (repo.locations/id-clause)
        (->> (repo.locations/modify {:locked? locked?}))
        (models/modify entities/locations ::repo.locations/model)
        (repos/exec! db))
    (->> location-id
         (repo.locations/id-clause)
         (select* db)
         (models.location-responses/with-location-responses db)
         (colls/only!))))

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
