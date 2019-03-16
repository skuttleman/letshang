(ns com.ben-allred.letshang.api.services.db.repositories.moment-responses
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]))

(defn select-by [clause]
  (-> entities/moment-responses
      (entities/select)
      (entities/with-alias :moment-responses)
      (assoc :where clause)))

(defn upsert [moment-response]
  (entities/upsert entities/moment-responses [moment-response] [:moment-id :user-id] [:response]))
