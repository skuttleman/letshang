(ns com.ben-allred.letshang.api.routes.moments
  (:require
    [com.ben-allred.letshang.api.services.db.models.moments :as models.moments]
    [com.ben-allred.letshang.api.services.handlers :refer [PATCH context]]
    [com.ben-allred.letshang.common.resources.core :as res]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]
    [compojure.core :refer [defroutes]]))

(def ^:private transform-spec
  {:moment-id uuids/->uuid})

(def ^:private save-spec
  {:data {:response res/response-validator}})

(defroutes routes
  (context "/moments" []
    (PATCH "/:moment-id"
           ^{:request-spec save-spec
             :transformer  transform-spec}
           {{:keys [moment-id]} :params :keys [auth/user body db]}
      (if-let [moment-response (models.moments/set-response db moment-id (get-in body [:data :response]) (:id user))]
        [:http.status/ok {:data moment-response}]
        [:http.status/not-found {:message "Invitation not found for user"}]))))
