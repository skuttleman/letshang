(ns com.ben-allred.letshang.api.services.middleware
  (:require
    [clojure.string :as string]
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.api.utils.jwt :as jwt]
    [com.ben-allred.letshang.api.utils.respond :as respond]
    [com.ben-allred.letshang.common.services.content :as content]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.transit :as transit])
  (:import
    (clojure.lang ExceptionInfo)
    (java.util Date)))

(defn ^:private resource? [uri]
  (or (= "/" uri) (re-find #"(^/js|^/css|^/images|^/favicon)" uri)))

(defn ^:private api? [{:keys [uri websocket?]}]
  (and (not websocket?)
       (re-find #"(^/api|^/health)" uri)))

(defn log-response [handler]
  (fn [request]
    (let [start (Date.)
          response (handler request)
          end (Date.)
          uri (:uri request)]
      (when-not (resource? uri)
        (log/info (format "[%d](%dms) %s: %s"
                          (or (:status response) 404)
                          (- (.getTime end) (.getTime start))
                          (string/upper-case (name (:request-method request)))
                          uri)))
      response)))

(defn content-type [handler]
  (fn [{:keys [headers] :as request}]
    (-> request
        (content/parse (get headers "content-type"))
        (handler)
        (cond-> (api? request) (content/prepare #{"content-type"} (get headers "accept"))))))

(defn auth [handler]
  (fn [{:keys [uri headers] :as request}]
    (let [user (when (or (string/starts-with? uri "/api")
                            (re-find #"text/html" (str (get headers "accept"))))
                    (some-> request
                            (get-in [:cookies "auth-token" :value])
                            (jwt/decode)
                            (:data)
                            (transit/parse)))]
      (-> request
          (cond-> user (assoc :auth/user user))
          (handler)))))

(defn abortable [handler]
  (fn [request]
    (try
      (handler request)
      (catch ExceptionInfo ex
        (if-let [response (:response (.getData ex))]
          response
          (throw ex))))))

(defn restricted [handler]
  (fn [{:keys [auth/user] :as request}]
    (if user
      (handler request)
      (respond/with [:http.status/forbidden {:message "You must authenticate to use this API."}]))))

(defn conform-params [handler spec]
  (if-not spec
    handler
    (let [transformer (f/transformer spec)]
      (fn [request]
        (-> request
            (update :params transformer)
            (handler))))))
