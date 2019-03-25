(ns com.ben-allred.letshang.api.routes.hangouts
  (:require
    [com.ben-allred.letshang.api.services.db.models.hangouts :as models.hangouts]
    [com.ben-allred.letshang.api.services.db.models.invitations :as models.invitations]
    [com.ben-allred.letshang.api.services.db.models.locations :as models.locations]
    [com.ben-allred.letshang.api.services.db.models.messages :as models.messages]
    [com.ben-allred.letshang.api.services.db.models.moments :as models.moments]
    [com.ben-allred.letshang.api.services.handlers :refer [GET PATCH POST context]]
    [com.ben-allred.letshang.common.resources.hangouts :as res.hangouts]
    [com.ben-allred.letshang.common.resources.hangouts.conversations :as res.conversations]
    [com.ben-allred.letshang.common.resources.hangouts.suggestions :as res.suggestions]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]
    [compojure.core :refer [defroutes]]))

(def ^:private message-spec
  {:data res.conversations/message-validator})

(def ^:private save-spec
  {:data res.hangouts/validator})

(def ^:private transform-spec
  {:hangout-id uuids/->uuid})

(def ^:private when-spec
  {:data res.suggestions/when-validator})

(def ^:private where-spec
  {:data res.suggestions/where-validator})

(def ^:private who-spec
  {:data res.suggestions/who-validator})

(defroutes routes
  (context "/hangouts" []
    (POST "/"
          ^{:request-spec save-spec}
          {:keys [auth/user body db]}
      (->> (:id user)
           (models.hangouts/create db (:data body))
           (hash-map :data)
           (conj [:http.status/created])))

    (GET "/"
         {:keys [auth/user db]}
      (->> (:id user)
           (models.hangouts/select-for-user db)
           (hash-map :data)
           (conj [:http.status/ok])))

    (context "/:hangout-id" ^{:transformer transform-spec} _
      (PATCH "/" ^{:request-spec save-spec} {{:keys [hangout-id]} :params :keys [auth/user body db]}
        (if-let [hangout (models.hangouts/modify db hangout-id (:data body) (:id user))]
          [:http.status/ok {:data hangout}]
          [:http.status/not-found {:message "Hangout not found for user"}]))

      (GET "/" {{:keys [hangout-id]} :params :keys [auth/user db]}
        (if-let [hangout (models.hangouts/find-for-user db hangout-id (:id user))]
          [:http.status/ok {:data hangout}]
          [:http.status/not-found {:message "Hangout not found for user"}]))

      (context "/invitations" _
        (POST "/" ^{:request-spec who-spec} {{:keys [hangout-id]} :params :keys [auth/user body db]}
          (if-let [suggestion (models.invitations/suggest-invitees db hangout-id (:data body) (:id user))]
            [:http.status/created {:data suggestion}]
            [:http.status/not-found {:message "Cannot suggest who for this hangout"}]))

        (GET "/" {{:keys [hangout-id]} :params :keys [auth/user db]}
          (if-let [invitations (models.invitations/select-for-hangout db hangout-id (:id user))]
            [:http.status/ok {:data invitations}]
            [:http.status/not-found {:message "Hangout not found for user"}])))

      (context "/locations" _
        (POST "/" ^{:request-spec where-spec} {{:keys [hangout-id]} :params :keys [auth/user body db]}
          (if-let [suggestion (models.locations/suggest-location db hangout-id (:data body) (:id user))]
            [:http.status/created {:data suggestion}]
            [:http.status/not-found {:message "Cannot suggest where for this hangout"}]))

        (GET "/" {{:keys [hangout-id]} :params :keys [auth/user db]}
          (if-let [locations (models.locations/select-for-hangout db hangout-id (:id user))]
            [:http.status/ok {:data locations}]
            [:http.status/not-found {:message "Hangout not found for user"}])))

      (context "/messages" _
        (POST "/" ^{:request-spec message-spec} {{:keys [hangout-id]} :params :keys [auth/user body db]}
          (if-let [message (models.messages/create db hangout-id (:data body) (:id user))]
            [:http.status/created {:data message}]
            [:http.status/not-found {:message "Message could not be created for this hangout"}]))

        (GET "/" {{:keys [hangout-id]} :params :keys [auth/user db]}
          (if-let [messages (models.messages/select-for-hangout db hangout-id (:id user))]
            [:http.status/created {:data messages}]
            [:http.status/not-found {:message "Cannot suggests when for this hangout"}])))

      (context "/moments" _
        (POST "/" ^{:request-spec when-spec} {{:keys [hangout-id]} :params :keys [auth/user body db]}
          (if-let [suggestion (models.moments/suggest-moment db hangout-id (:data body) (:id user))]
            [:http.status/created {:data suggestion}]
            [:http.status/not-found {:message "Cannot suggest when for this hangout"}]))

        (GET "/" {{:keys [hangout-id]} :params :keys [auth/user db]}
          (if-let [moments (models.moments/select-for-hangout db hangout-id (:id user))]
            [:http.status/ok {:data moments}]
            [:http.status/not-found {:message "Hangout not found for user"}]))))))
