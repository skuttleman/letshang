(ns com.ben-allred.letshang.api.routes.invitations
  (:require
    [com.ben-allred.letshang.api.services.db.models.invitations :as models.invitations]
    [com.ben-allred.letshang.api.services.ws :as ws]
    [com.ben-allred.letshang.common.resources.hangouts.responses :as res.responses]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]))

(def ^:private transform-spec
  {:invitation-id uuids/->uuid})

(def ^:private save-spec
  {:data {:response res.responses/response-validator}})

(defn response [{{:keys [invitation-id]} :params {user-id :id} :auth/user :keys [body db]}]
  (if-let [{:keys [hangout-id] :as response} (models.invitations/set-response db
                                                                              invitation-id
                                                                              (get-in body [:data :response])
                                                                              user-id)]
    (do (ws/publish! :topic
                     [:hangout hangout-id]
                     [:hangout.invitation/response {:data response}])
        [:http.status/ok {:data response}])
    [:http.status/not-found {:message "Invitation not found for user"}]))

(def routes
  {:api/invitation.responses ^{:transformer transform-spec :request-spec save-spec}
                             {:put #'response}})
