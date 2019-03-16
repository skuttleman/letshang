(ns com.ben-allred.letshang.api.services.db.repositories.location-responses
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]))

(defn select-by [clause]
  (-> entities/location-responses
      (entities/select)
      (entities/with-alias :location-responses)
      (assoc :where clause)))

(defn upsert [location-response]
  (entities/upsert entities/location-responses [location-response] [:location-id :user-id] [:response]))

(defn hangout-ids-clause
  ([clause hangout-ids]
   [:and clause (hangout-ids-clause hangout-ids)])
  ([hangout-ids]
   [:in :location-responses.hangout-id hangout-ids]))
