(ns com.ben-allred.letshang.api.routes.core
  (:require
    [com.ben-allred.letshang.api.routes.auth :as auth]
    [com.ben-allred.letshang.api.routes.hangouts :as hangouts]
    [com.ben-allred.letshang.api.routes.invitations :as invitations]
    [com.ben-allred.letshang.api.routes.locations :as locations]
    [com.ben-allred.letshang.api.routes.moments :as moments]
    [com.ben-allred.letshang.api.routes.users :as users]
    [com.ben-allred.letshang.api.services.handlers :refer [GET ANY context]]
    [com.ben-allred.letshang.api.services.html :as html]
    [com.ben-allred.letshang.api.services.middleware :as middleware]
    [com.ben-allred.letshang.api.services.ws :as ws]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [compojure.core :refer [defroutes]]
    [compojure.route :as route]
    [ring.middleware.cookies :refer [wrap-cookies]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.multipart-params :refer [wrap-multipart-params]]
    [ring.middleware.nested-params :refer [wrap-nested-params]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.reload :refer [wrap-reload]]))

(defroutes api*
  #'hangouts/routes
  #'invitations/routes
  #'locations/routes
  #'moments/routes
  #'users/routes
  (GET "/events" req
    (ws/connect req)))

(def ^:private api
  (-> #'api*
      (#'middleware/with-authentication)))

(defroutes ^:private base
  (context "/api" []
    #'api
    (ANY "/*" [] [:http.status/not-found]))
  (context "/" []
    #'auth/routes
    (route/resources "/")
    (GET "/health" [] [:http.status/ok {:a :ok}])
    (GET "/*" req [:http.status/ok
                   (-> req
                       (select-keys #{:auth/sign-up :auth/user :query-string :uri})
                       (html/render))
                   {"content-type" "text/html"}])
    (ANY "/*" [] [:http.status/not-found])))

(def app
  (-> #'base
      (#'middleware/with-transaction)
      (#'middleware/with-jwt)
      (#'middleware/with-logging)
      (wrap-multipart-params)
      (wrap-keyword-params)
      (wrap-nested-params)
      (wrap-params)
      (wrap-cookies)
      (#'middleware/with-abortable)
      (#'middleware/with-content-type)))

(def app-dev
  (-> #'app
      (wrap-reload {:dirs ["src/clj" "src/cljc"]})))
