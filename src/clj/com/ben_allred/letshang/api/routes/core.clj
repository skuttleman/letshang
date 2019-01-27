(ns com.ben-allred.letshang.api.routes.core
  (:require
    [com.ben-allred.letshang.api.routes.auth :as auth]
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

(defroutes ^:private base
  (context "/" []
    auth/routes
    (route/resources "/")
    (GET "/health" [] [:http.status/ok {:a :ok}])
    (GET "/*" req [:http.status/ok
                   (-> req
                       (select-keys #{:uri :query-string :user})
                       (html/render))
                   {"content-type" "text/html"}])
    (ANY "/*" [] [:http.status/not-found])))

(def app
  (-> #'base
      (wrap-reload {:dirs ["src/clj" "src/cljc"]})
      (#'middleware/auth)
      (#'middleware/abortable)
      (#'middleware/content-type)
      (#'middleware/log-response)
      (wrap-keyword-params)
      (wrap-nested-params)
      (wrap-params)
      (wrap-cookies)))
