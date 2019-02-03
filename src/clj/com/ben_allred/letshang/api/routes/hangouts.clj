(ns com.ben-allred.letshang.api.routes.hangouts
  (:require
    [com.ben-allred.letshang.api.services.db.models.hangouts :as models.hangouts]
    [com.ben-allred.letshang.api.services.handlers :refer [GET context]]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]
    [compojure.core :refer [defroutes]]))

(def ^:private transform-spec
  {:hangout-id uuids/->uuid})

(defroutes routes
  (context "/hangouts" []
    (GET "/" {:keys [auth/user]}
      (->> (:id user)
           (models.hangouts/select-for-user)
           (hash-map :data)
           (conj [:http.status/ok])))
    (GET "/:hangout-id" ^{:transformer transform-spec} {{:keys [hangout-id]} :params :keys [auth/user]}
      (if-let [hangout (models.hangouts/find-for-user hangout-id (:id user))]
        [:http.status/ok {:data hangout}]
        [:http.status/not-found {:message "Hangout not found"}]))))
