(ns com.ben-allred.letshang.api.routes.moments
  (:require
    [com.ben-allred.letshang.api.services.db.models.moments :as models.moments]
    [com.ben-allred.letshang.api.services.handlers :refer [PATCH POST context]]
    [com.ben-allred.letshang.common.resources.hangouts.responses :as res.responses]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]
    [compojure.core :refer [defroutes]]
    [com.ben-allred.letshang.common.resources.hangouts.locks :as res.locks]))

(def ^:private transform-spec
  {:moment-id uuids/->uuid})

(def ^:private response-spec
  {:data {:response res.responses/response-validator}})

(def ^:private lock-spec
  {:data {:locked? res.locks/lock-validator}})

(defroutes routes
  (context "/moments/:moment-id" ^{:transformer transform-spec} _
    (POST "/responses" ^{:request-spec lock-spec} {{:keys [moment-id]} :params :keys [auth/user body db]}
      (if-let [moment-response (models.moments/set-response db
                                                            moment-id
                                                            (get-in body [:data :response])
                                                            (:id user))]
        [:http.status/ok {:data moment-response}]
        [:http.status/not-found {:message "Invitation not found for user"}]))
    (PATCH "/" ^{:request-spec lock-spec} {{:keys [moment-id]} :params :keys [auth/user body db]}
      (if-let [moment (models.moments/lock-moment db
                                                  moment-id
                                                  (get-in body [:data :locked?])
                                                  (:id user))]
        [:http.status/ok {:data moment}]
        [:http.status/not-found {:message "Invitation not found for user"}]))))
