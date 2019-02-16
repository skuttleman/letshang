(ns com.ben-allred.letshang.api.routes.users
  (:require
    [com.ben-allred.letshang.api.services.db.models.users :as models.users]
    [com.ben-allred.letshang.api.services.handlers :refer [GET POST context]]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [compojure.core :refer [defroutes]]))

(defroutes routes
  (context "/users" []
    (GET "/associates" {:keys [auth/user db]}
      (->> (:id user)
           (models.users/find-known-associates db)
           (hash-map :data)
           (conj [:http.status/ok])))))
