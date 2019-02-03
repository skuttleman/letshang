(ns com.ben-allred.letshang.api.routes.core
  (:require
    [com.ben-allred.letshang.api.routes.auth :as auth]
    [com.ben-allred.letshang.api.routes.hangouts :as hangouts]
    [com.ben-allred.letshang.api.services.html :as html]
    [com.ben-allred.letshang.api.services.middleware :as middleware]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [compojure.core :refer [ANY GET context defroutes]]
    [compojure.route :as route]
    [ring.middleware.cookies :refer [wrap-cookies]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.nested-params :refer [wrap-nested-params]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.reload :refer [wrap-reload]]))

(defroutes api*
  #'hangouts/routes)

(def ^:private api
  (-> #'api*
      (#'middleware/restricted)))

(defroutes ^:private base
  (context "/api" []
    #'api)
  (context "/" []
    #'auth/routes
    (route/resources "/")
    (GET "/health" [] [:http.status/ok {:a :ok}])
    (GET "/*" req [:http.status/ok
                   (-> req
                       (select-keys #{:uri :query-string :auth/user})
                       (html/render))
                   {"content-type" "text/html"}])
    (ANY "/*" [] [:http.status/not-found])))

(def app
  (-> #'base
      (#'middleware/auth)
      (#'middleware/abortable)
      (#'middleware/content-type)
      (#'middleware/log-response)
      (wrap-keyword-params)
      (wrap-nested-params)
      (wrap-params)
      (wrap-cookies)))

(def app-dev
  (-> #'app
      (wrap-reload {:dirs ["src/clj" "src/cljc"]})))
