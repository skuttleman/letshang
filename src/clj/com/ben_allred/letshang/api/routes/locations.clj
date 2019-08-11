(ns com.ben-allred.letshang.api.routes.locations
  (:require
    [com.ben-allred.letshang.api.services.db.models.locations :as models.locations]
    [com.ben-allred.letshang.api.services.handlers :refer [PATCH POST context]]
    [com.ben-allred.letshang.common.resources.hangouts.locks :as res.locks]
    [com.ben-allred.letshang.common.resources.hangouts.responses :as res.responses]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]
    [compojure.core :refer [defroutes]]))

(def ^:private transform-spec
  {:location-id uuids/->uuid})

(def ^:private response-spec
  {:data {:response res.responses/response-validator}})

(def ^:private lock-spec
  {:data res.locks/lock-validator})

(defroutes routes
  (context "/locations/:location-id" ^{:transformer transform-spec} _
    (POST "/responses" ^{:request-spec lock-spec} {{:keys [location-id]} :params :keys [auth/user body db]}
      (if-let [location-response (models.locations/set-response db
                                                                location-id
                                                                (get-in body [:data :response])
                                                                (:id user))]
        [:http.status/ok {:data location-response}]
        [:http.status/not-found {:message "Invitation not found for user"}]))
    (PATCH "/" ^{:request-spec lock-spec} {{:keys [location-id]} :params :keys [auth/user body db]}
      (if-let [location (models.locations/lock-location db
                                                        location-id
                                                        (get-in body [:data :locked?])
                                                        (:id user))]
        [:http.status/ok {:data location}]
        [:http.status/not-found {:message "Invitation not found for user"}]))))
