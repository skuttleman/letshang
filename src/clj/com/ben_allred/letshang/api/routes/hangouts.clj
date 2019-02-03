(ns com.ben-allred.letshang.api.routes.hangouts
  (:require
    [compojure.core :refer [ANY DELETE GET POST PUT context defroutes]]
    [com.ben-allred.letshang.api.services.db.models.hangouts :as models.hangouts]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defroutes routes
  (context "/hangouts" []
    (GET "/" {:keys [auth/user]}
      (->> (:id user)
           (models.hangouts/select-for-user)
           (hash-map :data)
           (conj [:http.status/ok])))))
