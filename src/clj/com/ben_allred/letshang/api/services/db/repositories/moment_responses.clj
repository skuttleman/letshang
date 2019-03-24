(ns com.ben-allred.letshang.api.services.db.repositories.moment-responses
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.preparations :as prep]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.common.utils.maps :as maps]))

(defmethod repos/->db ::model
  [_ moment]
  (dissoc moment :created-at))

(defmethod repos/->api ::model
  [_ moment]
  (maps/update-maybe moment :response keyword))

(defmethod repos/->sql-value [:moment-responses :response]
  [_ _ value]
  (prep/user-response value))

(defn select-by [clause]
  (-> entities/moment-responses
      (entities/select)
      (entities/with-alias :moment-responses)
      (assoc :where clause)))

(defn upsert [moment-response]
  (entities/upsert entities/moment-responses [moment-response] [:moment-id :user-id] [:response]))

(defn moment-ids-clause
  ([clause moment-ids]
   [:and clause (moment-ids-clause moment-ids)])
  ([moment-ids]
   [:in :moment-responses.moment-id moment-ids]))
