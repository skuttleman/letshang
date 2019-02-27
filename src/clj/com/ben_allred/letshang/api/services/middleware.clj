(ns com.ben-allred.letshang.api.services.middleware
  (:require
    [clojure.string :as string]
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.utils.respond :as respond]
    [com.ben-allred.letshang.common.services.content :as content]
    [com.ben-allred.letshang.common.utils.encoders.jwt :as jwt]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps])
  (:import
    (clojure.lang ExceptionInfo)
    (java.util Date)))

(defn ^:private api? [{:keys [uri websocket?]}]
  (and (not websocket?)
       (or (= "/auth/register" uri)
           (re-find #"(^/api|^/health)" uri))))

(defn log-response [handler]
  (fn [request]
    (let [start (Date.)
          response (handler request)
          end (Date.)]
      (when (api? request)
        (log/info (format "[%d](%dms) %s: %s"
                          (or (:status response) 404)
                          (- (.getTime end) (.getTime start))
                          (string/upper-case (name (:request-method request)))
                          (:uri request))))
      response)))

(defn content-type [handler]
  (fn [{:keys [headers] :as request}]
    (-> request
        (content/parse (get headers "content-type"))
        (handler)
        (cond-> (api? request) (content/prepare #{"content-type"} (get headers "accept"))))))

(defn auth [handler]
  (fn [{:keys [uri headers] :as request}]
    (let [{:keys [user sign-up]} (when (or (string/starts-with? uri "/api")
                                           (string/starts-with? uri "/auth")
                                           (re-find #"text/html" (str (get headers "accept"))))
                                   (some-> request
                                           (get-in [:cookies "auth-token" :value])
                                           (jwt/decode)
                                           (:data)))]
      (-> request
          (maps/assoc-maybe :auth/user user :auth/sign-up sign-up)
          (handler)))))

(defn with-transaction [handler]
  (fn [request]
    (repos/transact
      (fn [db]
        (handler (assoc request :db db))))))

(defn abortable [handler]
  (fn [request]
    (try
      (handler request)
      (catch ExceptionInfo ex
        (if-let [response (:response (.getData ex))]
          (update response :status #(or % 500))
          (throw ex))))))

(defn restricted [handler]
  (fn [request]
    (if (:auth/user request)
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

(defn validate-body! [handler spec]
  (if-not spec
    handler
    (let [validator (f/validator spec)]
      (fn [request]
        (when-let [errors (validator (:body request))]
          (respond/abort! [:http.status/bad-request {:message "Request does not meet spec"
                                                     :errors  errors}]))
        (handler request)))))
