(ns com.ben-allred.letshang.api.routes.invitations
  (:require
    [com.ben-allred.letshang.api.services.db.models.invitations :as models.invitations]
    [com.ben-allred.letshang.api.services.handlers :refer [PUT context]]
    [com.ben-allred.letshang.common.resources.hangouts.responses :as res.responses]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]
    [compojure.core :refer [defroutes]]))

(def ^:private transform-spec
  {:invitation-id uuids/->uuid})

(def ^:private save-spec
  {:data {:response res.responses/response-validator}})

(defroutes routes
  (context "/invitations/:invitation-id" ^{:transformer transform-spec} _
    (PUT "/responses" ^{:request-spec save-spec} {{:keys [invitation-id]} :params :keys [auth/user body db]}
      (if (models.invitations/set-response db invitation-id (get-in body [:data :response]) (:id user))
        [:http.status/no-content]
        [:http.status/not-found {:message "Invitation not found for user"}]))))
