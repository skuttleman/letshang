(ns com.ben-allred.letshang.api.routes.core
  (:require
    [com.ben-allred.letshang.api.routes.auth :as auth]
    [com.ben-allred.letshang.api.routes.hangouts :as hangouts]
    [com.ben-allred.letshang.api.routes.invitations :as invitations]
    [com.ben-allred.letshang.api.routes.locations :as locations]
    [com.ben-allred.letshang.api.routes.moments :as moments]
    [com.ben-allred.letshang.api.routes.users :as users]
    [com.ben-allred.letshang.api.services.handlers :as handlers]
    [com.ben-allred.letshang.api.services.middleware :as middleware]
    [com.ben-allred.letshang.api.services.navigation :as nav]
    [com.ben-allred.letshang.api.services.ws :as ws]
    [com.ben-allred.letshang.api.utils.respond :as respond]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [ring.middleware.cookies :refer [wrap-cookies]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.multipart-params :refer [wrap-multipart-params]]
    [ring.middleware.nested-params :refer [wrap-nested-params]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.reload :refer [wrap-reload]]))

(defn ^:private update-maps [m f & f-args]
  (cond-> m
    (map? m) (as-> $
                   (apply maps/update-all $ f f-args)
                   (with-meta $ (meta m)))))

(def ^:private base
  {:ui/*              {:get #'handlers/home}
   :resources/health  {:get #'handlers/health}
   :resources/js      {:get #'handlers/resources}
   :resources/css     {:get #'handlers/resources}
   :resources/images  {:get #'handlers/resources}
   :resources/favicon {:get #'handlers/resources}})

(def ^:private handlers
  (-> (merge hangouts/routes
             invitations/routes
             locations/routes
             moments/routes
             users/routes
             ws/routes)
      (update-maps update-maps middleware/with-authentication)
      (merge auth/routes base)
      (handlers/wrap-meta)))

(defn ^:private app* [req]
  (let [{:keys [route-params handler query-params]} (nav/match-route (:uri req))
        handler (if (= "ui" (namespace handler))
                  :ui/*
                  handler)]
    (if-let [handler (some-> handlers
                             (apply [handler])
                             (apply [(:request-method req)]))]
      (-> req
          (update :params merge route-params query-params)
          (update :query-params merge query-params)
          (handler)
          (respond/with))
      (respond/with [:http.status/not-found]))))

(def app
  (-> #'app*
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
