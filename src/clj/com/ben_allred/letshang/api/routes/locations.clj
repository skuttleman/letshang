(ns com.ben-allred.letshang.api.routes.locations
  (:require
    [com.ben-allred.letshang.api.services.db.models.locations :as models.locations]
    [com.ben-allred.letshang.api.services.handlers :refer [PATCH context]]
    [com.ben-allred.letshang.common.resources.core :as res]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]
    [compojure.core :refer [defroutes]]))

(def ^:private transform-spec
  {:location-id uuids/->uuid})

(def ^:private save-spec
  {:data {:response res/response-validator}})

(defroutes routes
  (context "/locations" []
    (PATCH "/:location-id"
           ^{:request-spec save-spec
             :transformer  transform-spec}
           {{:keys [location-id]} :params :keys [auth/user body db]}
      (if-let [location-response (models.locations/set-response db location-id (get-in body [:data :response]) (:id user))]
        [:http.status/ok {:data location-response}]
        [:http.status/not-found {:message "Invitation not found for user"}]))))
