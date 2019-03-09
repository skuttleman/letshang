(ns com.ben-allred.letshang.api.routes.hangouts
  (:require
    [com.ben-allred.letshang.api.services.db.models.hangouts :as models.hangouts]
    [com.ben-allred.letshang.api.services.db.models.locations :as models.locations]
    [com.ben-allred.letshang.api.services.db.models.moments :as models.moments]
    [com.ben-allred.letshang.api.services.handlers :refer [GET PATCH POST context]]
    [com.ben-allred.letshang.common.resources.hangouts :as res.hangouts]
    [com.ben-allred.letshang.common.resources.hangouts.suggestions :as res.suggestions]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]
    [compojure.core :refer [defroutes]]))

(def ^:private transform-spec
  {:hangout-id uuids/->uuid})

(def ^:private save-spec
  {:data res.hangouts/validator})

(def ^:private when-spec
  {:data res.suggestions/when-validator})

(def ^:private where-spec
  {:data res.suggestions/where-validator})

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
      (PATCH "/"
             ^{:request-spec save-spec}
             {{:keys [hangout-id]} :params :keys [auth/user body db]}
        (if-let [hangout (models.hangouts/modify db hangout-id (:data body) (:id user))]
          [:http.status/ok {:data hangout}]
          [:http.status/not-found {:message "Hangout not found for user"}]))
      (GET "/"
           {{:keys [hangout-id]} :params :keys [auth/user db]}
        (if-let [hangout (models.hangouts/find-for-user db hangout-id (:id user))]
          [:http.status/ok {:data hangout}]
          [:http.status/not-found {:message "Hangout not found"}]))
      (context "/suggestions" _
        (POST "/when" ^{:request-spec when-spec} {{:keys [hangout-id]} :params :keys [auth/user body db]}
          (when-let [suggestion (models.moments/suggest-moment db hangout-id (:data body) (:id user))]
            [:http.status/created {:data suggestion}]))
        (POST "/where" ^{:request-spec where-spec} {{:keys [hangout-id]} :params :keys [auth/user body db]}
          (when-let [suggestion (models.locations/suggest-location db hangout-id (:data body) (:id user))]
            [:http.status/created {:data suggestion}]))))))
