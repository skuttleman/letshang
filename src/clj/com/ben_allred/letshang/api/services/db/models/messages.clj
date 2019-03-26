(ns com.ben-allred.letshang.api.services.db.models.messages
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.hangouts :as repo.hangouts]
    [com.ben-allred.letshang.api.services.db.repositories.messages :as repo.messages]
    [com.ben-allred.letshang.common.utils.colls :as colls]))

(defn ^:private select*
  ([db clause]
   (select* db nil clause))
  ([db {:keys [limit offset]} clause]
   (-> clause
       (repo.messages/select-by)
       (entities/inner-join entities/users :creator [:= :creator.id :messages.created-by])
       (entities/order :messages.created-at :desc)
       (cond->
         offset (entities/offset offset)
         limit (entities/limit limit))
       (models/select ::repo.messages/model (models/under :messages))
       (repos/exec! db))))

(defn select-for-hangout [db hangout-id user-id pagination]
  (when (-> hangout-id
            (repo.hangouts/id-clause)
            (repo.hangouts/has-hangout-clause user-id)
            (repo.hangouts/select-by)
            (models/select ::repo.hangouts/model)
            (repos/exec! db)
            (colls/only!))
    (-> hangout-id
        (repo.messages/hangout-id-clause)
        (->> (select* db pagination)))))

(defn create [db hangout-id message user-id]
  (let [message (assoc message :created-by user-id :hangout-id hangout-id)
        message-id (-> message
                       (repo.messages/insert)
                       (models/insert-many entities/messages ::repo.messages/model)
                       (repos/exec! db)
                       (colls/only!)
                       (:id))]
    (->> message-id
         (repo.messages/id-clause)
         (select* db)
         (colls/only!))))
