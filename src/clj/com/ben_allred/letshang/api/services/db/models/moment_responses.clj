(ns com.ben-allred.letshang.api.services.db.models.moment-responses
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.preparations :as prep]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.moment-responses :as repo.moment-responses]
    [com.ben-allred.letshang.common.utils.maps :as maps]))

(defmethod models/->db ::model
  [_ moment]
  (dissoc moment :created-at))

(defmethod models/->api ::model
  [_ moment]
  (maps/update-maybe moment :response keyword))

(def ^:private prepare-response (prep/for-type :user-response))

(defmethod repos/->sql-value [:moment-responses :response]
  [_ _ value]
  (prepare-response value))

(defn respond [db moment-response]
  (-> moment-response
      (repo.moment-responses/upsert)
      (models/insert-many entities/moment-responses ::model)
      (repos/exec! db))
  moment-response)
