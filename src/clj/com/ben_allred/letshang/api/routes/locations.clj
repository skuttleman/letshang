(ns com.ben-allred.letshang.api.routes.locations
  (:require
    [com.ben-allred.letshang.api.services.db.models.locations :as models.locations]
    [com.ben-allred.letshang.api.services.handlers :refer [PATCH PUT context]]
    [com.ben-allred.letshang.api.services.ws :as ws]
    [com.ben-allred.letshang.common.resources.hangouts.responses :as res.responses]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]
    [compojure.core :refer [defroutes]]))

(def ^:private transform-spec
  {:location-id uuids/->uuid})

(def ^:private response-spec
  {:data {:response res.responses/response-validator}})

(defroutes routes
  (context "/locations/:location-id" ^{:transformer transform-spec} _
    (PUT "/responses" ^{:request-spec response-spec} {{:keys [location-id]} :params :keys [auth/user body db]}
      (if-let [{:keys [hangout-id] :as response} (models.locations/set-response db
                                                                                location-id
                                                                                (get-in body [:data :response])
                                                                                (:id user))]
        (do (ws/publish! :topic
                         [:hangout hangout-id]
                         [:hangout.location/response {:data response}])
            [:http.status/ok {:data response}])
        [:http.status/not-found {:message "Invitation not found for user"}]))))
