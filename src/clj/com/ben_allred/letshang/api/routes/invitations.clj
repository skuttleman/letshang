(ns com.ben-allred.letshang.api.routes.invitations
  (:require
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.api.services.db.models.invitations :as models.invitations]
    [com.ben-allred.letshang.api.services.handlers :refer [PATCH context]]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]
    [compojure.core :refer [defroutes]]))

(def ^:private transform-spec
  {:invitation-id uuids/->uuid})

(def ^:private save-spec
  {:data {:response (f/pred #{:positive :negative :neutral} "Must specify a response")}})

(defroutes routes
  (context "/invitations" []
    (PATCH "/:invitation-id"
           ^{:request-spec save-spec
             :transformer  transform-spec}
           {{:keys [invitation-id]} :params :keys [auth/user body db]}
      (if (models.invitations/set-response db invitation-id (get-in body [:data :response]) (:id user))
        [:http.status/no-content]
        [:http.status/not-found {:message "Invitation not found for user"}]))))
