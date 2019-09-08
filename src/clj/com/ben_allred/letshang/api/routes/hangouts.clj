(ns com.ben-allred.letshang.api.routes.hangouts
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.letshang.api.services.db.models.hangouts :as models.hangouts]
    [com.ben-allred.letshang.api.services.db.models.invitations :as models.invitations]
    [com.ben-allred.letshang.api.services.db.models.locations :as models.locations]
    [com.ben-allred.letshang.api.services.db.models.messages :as models.messages]
    [com.ben-allred.letshang.api.services.db.models.moments :as models.moments]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.handlers :refer [GET PATCH POST PUT context]]
    [com.ben-allred.letshang.api.services.ws :as ws]
    [com.ben-allred.letshang.common.resources.hangouts :as res.hangouts]
    [com.ben-allred.letshang.common.resources.hangouts.conversations :as res.conversations]
    [com.ben-allred.letshang.common.resources.hangouts.suggestions :as res.suggestions]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.numbers :as numbers]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]
    [compojure.core :refer [defroutes]]))

(def ^:private message-spec
  {:data res.conversations/message-validator})

(def ^:private save-spec
  {:data res.hangouts/validator})

(def ^:private transform-spec
  {:hangout-id uuids/->uuid
   :offset     numbers/parse-int})

(def ^:private when-spec
  {:data res.suggestions/when-validator})

(def ^:private where-spec
  {:data res.suggestions/where-validator})

(def ^:private who-spec
  {:data res.suggestions/who-validator})

(defn ^:private with-user-publish [[_ body :as response] event user-ids]
  (async/go
    (doseq [user-id user-ids]
      (ws/publish! :user user-id [event body])))
  response)

(defn ^:private with-hangout-publish [[_ body :as response] event hangout-id]
  (async/go
    (ws/publish! :topic [:hangout hangout-id] [event body]))
  response)

(defn ^:private with-invitation-publish [[_ {:keys [data]} :as response] event hangout-id user-id]
  (async/go
    (let [message (delay
                    (repos/transact
                      (fn [db]
                        [event {:data (models.hangouts/find-for-user db hangout-id user-id)}])))]
      (doseq [user-id (map :user-id data)]
        (ws/publish! :user user-id message))))
  response)

(defroutes routes
  (context "/hangouts" []
    (POST "/"
          ^{:request-spec save-spec}
          {{:keys [data]} :body {user-id :id} :auth/user :keys [db]}
      (with-user-publish [:http.status/created {:data (models.hangouts/create db data user-id)}]
                         :hangouts/new
                         (conj (:invitation-ids data) user-id)))

    (GET "/"
         {:keys [auth/user db]}
      (->> (:id user)
           (models.hangouts/select-for-user db)
           (hash-map :data)
           (conj [:http.status/ok])))

    (context "/:hangout-id" ^{:transformer transform-spec} _
      (PATCH "/" ^{:request-spec save-spec} {{:keys [hangout-id]} :params :keys [auth/user body db]}
        (if-let [hangout (models.hangouts/modify db hangout-id (:data body) (:id user))]
          (with-hangout-publish [:http.status/ok {:data hangout}] :hangout/updated hangout-id)
          [:http.status/not-found {:message "Hangout not found for user"}]))

      (GET "/" {{:keys [hangout-id]} :params :keys [auth/user db]}
        (if-let [hangout (models.hangouts/find-for-user db hangout-id (:id user))]
          [:http.status/ok {:data hangout}]
          [:http.status/not-found {:message "Hangout not found for user"}]))

      (context "/invitations" _
        (PUT "/" ^{:request-spec who-spec} {{:keys [hangout-id]} :params {user-id :id} :auth/user :keys [body db]}
          (if-let [suggestion (models.invitations/suggest-invitees db hangout-id (:data body) user-id)]
            (with-invitation-publish [:http.status/created {:data suggestion}] :hangouts/invited hangout-id user-id)
            [:http.status/not-found {:message "Cannot suggest who for this hangout"}]))

        (GET "/" {{:keys [hangout-id]} :params :keys [auth/user db]}
          (if-let [invitations (models.invitations/select-for-hangout db hangout-id (:id user))]
            [:http.status/ok {:data invitations}]
            [:http.status/not-found {:message "Hangout not found for user"}])))

      (context "/locations" _
        (PUT "/" ^{:request-spec where-spec} {{:keys [hangout-id]} :params {user-id :id} :auth/user :keys [body db]}
          (if-let [suggestion (models.locations/suggest-location db hangout-id (:data body) user-id)]
            (with-hangout-publish [:http.status/created {:data suggestion}] :hangout.suggestion/location hangout-id)
            [:http.status/not-found {:message "Cannot suggest where for this hangout"}]))

        (GET "/" {{:keys [hangout-id]} :params :keys [auth/user db]}
          (if-let [locations (models.locations/select-for-hangout db hangout-id (:id user))]
            [:http.status/ok {:data locations}]
            [:http.status/not-found {:message "Hangout not found for user"}])))

      (context "/messages" _
        (POST "/" ^{:request-spec message-spec} {{:keys [hangout-id]} :params :keys [auth/user body db]}
          (if-let [message (models.messages/create db hangout-id (:data body) (:id user))]
            (with-hangout-publish [:http.status/created {:data message}] :messages/new hangout-id)
            [:http.status/not-found {:message "Message could not be created for this hangout"}]))

        (GET "/" {{:keys [hangout-id offset]} :params :keys [auth/user db]}
          (if-let [messages (models.messages/select-for-hangout db hangout-id (:id user) {:limit 20 :offset offset})]
            [:http.status/ok {:data messages}]
            [:http.status/not-found {:message "Cannot suggests when for this hangout"}])))

      (context "/moments" _
        (PUT "/" ^{:request-spec when-spec} {{:keys [hangout-id]} :params :keys [auth/user body db]}
          (if-let [suggestion (models.moments/suggest-moment db hangout-id (:data body) (:id user))]
            (with-hangout-publish [:http.status/created {:data suggestion}] :hangout.suggestion/moment hangout-id)
            [:http.status/not-found {:message "Cannot suggest when for this hangout"}]))

        (GET "/" {{:keys [hangout-id]} :params :keys [auth/user db]}
          (if-let [moments (models.moments/select-for-hangout db hangout-id (:id user))]
            [:http.status/ok {:data moments}]
            [:http.status/not-found {:message "Hangout not found for user"}]))))))
