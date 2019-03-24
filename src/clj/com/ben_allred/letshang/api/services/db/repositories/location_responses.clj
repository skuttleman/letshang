(ns com.ben-allred.letshang.api.services.db.repositories.location-responses
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.api.services.db.preparations :as prep]))

(defmethod repos/->db ::model
  [_ location]
  (dissoc location :created-at))

(defmethod repos/->api ::model
  [_ location]
  (maps/update-maybe location :response keyword))

(defmethod repos/->sql-value [:location-responses :response]
  [_ _ value]
  (prep/user-response value))

(defn select-by [clause]
  (-> entities/location-responses
      (entities/select)
      (entities/with-alias :location-responses)
      (assoc :where clause)))

(defn upsert [location-response]
  (entities/upsert entities/location-responses [location-response] [:location-id :user-id] [:response]))

(defn location-ids-clause
  ([clause location-ids]
   [:and clause (location-ids-clause location-ids)])
  ([location-ids]
   [:in :location-responses.location-id location-ids]))
