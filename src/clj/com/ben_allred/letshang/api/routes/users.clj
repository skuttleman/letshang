(ns com.ben-allred.letshang.api.routes.users
  (:require
    [com.ben-allred.letshang.api.services.db.models.users :as models.users]
    [com.ben-allred.letshang.api.services.handlers :refer [GET context]]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [compojure.core :refer [defroutes]]))

(defroutes routes
  (context "/users" []
    (GET "/associates" {:keys [auth/user]}
      (->> (:id user)
           (models.users/find-known-associates)
           (hash-map :data)
           (conj [:http.status/ok])))))
