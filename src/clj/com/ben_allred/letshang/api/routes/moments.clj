(ns com.ben-allred.letshang.api.routes.moments
  (:require
    [com.ben-allred.letshang.api.services.db.models.moments :as models.moments]
    [com.ben-allred.letshang.api.services.ws :as ws]
    [com.ben-allred.letshang.common.resources.hangouts.responses :as res.responses]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]))

(def ^:private transform-spec
  {:moment-id uuids/->uuid})

(def ^:private response-spec
  {:data {:response res.responses/response-validator}})

(defn response [{{:keys [moment-id]} :params :keys [auth/user body db]}]
  (if-let [{:keys [hangout-id] :as response} (models.moments/set-response db
                                                                          moment-id
                                                                          (get-in body [:data :response])
                                                                          (:id user))]
    (do (ws/publish! :topic
                     [:hangout hangout-id]
                     [:hangout.moment/response {:data response}])
        [:http.status/ok {:data response}])
    [:http.status/not-found {:message "Invitation not found for user"}]))

(def routes
  {:api/moment.responses ^{:transformer transform-spec :request-spec response-spec}
                         {:put #'response}})
