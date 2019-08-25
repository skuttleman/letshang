(ns com.ben-allred.letshang.common.services.http
  (:refer-clojure :exclude [get])
  (:require
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [#?(:clj clj-http.client :cljs cljs-http.client) :as client]
    [#?(:clj clj-http.core :cljs cljs-http.core) :as http*]
    [#?(:clj clojure.core.async :cljs cljs.core.async) :as async]
    [clojure.set :as set]
    [com.ben-allred.letshang.common.services.content :as content]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log #?@(:cljs [:include-macros true])]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.common.utils.serde.core :as serde]
    [com.ben-allred.letshang.common.utils.serde.edn :as edn]
    [com.ben-allred.letshang.common.utils.serde.json :as json]
    [com.ben-allred.letshang.common.utils.serde.transit :as transit]
    #?(:clj clj-http.cookies)))

(def ^:private header-keys #{:content-type :accept})

(def status->kw
  {200 :http.status/ok
   201 :http.status/created
   202 :http.status/accepted
   203 :http.status/non-authoritative-information
   204 :http.status/no-content
   205 :http.status/reset-content
   206 :http.status/partial-content
   300 :http.status/multiple-choices
   301 :http.status/moved-permanently
   302 :http.status/found
   303 :http.status/see-other
   304 :http.status/not-modified
   305 :http.status/use-proxy
   306 :http.status/unused
   307 :http.status/temporary-redirect
   400 :http.status/bad-request
   401 :http.status/unauthorized
   402 :http.status/payment-required
   403 :http.status/forbidden
   404 :http.status/not-found
   405 :http.status/method-not-allowed
   406 :http.status/not-acceptable
   407 :http.status/proxy-authentication-required
   408 :http.status/request-timeout
   409 :http.status/conflict
   410 :http.status/gone
   411 :http.status/length-required
   412 :http.status/precondition-failed
   413 :http.status/request-entity-too-large
   414 :http.status/request-uri-too-long
   415 :http.status/unsupported-media-type
   416 :http.status/requested-range-not-satisfiable
   417 :http.status/expectation-failed
   500 :http.status/internal-server-error
   501 :http.status/not-implemented
   502 :http.status/bad-gateway
   503 :http.status/service-unavailable
   504 :http.status/gateway-timeout
   505 :http.status/http-version-not-supported})

(def kw->status
  (set/map-invert status->kw))

(defn ^:private check-status [lower upper response]
  (when-let [status (if (vector? response)
                      (kw->status (clojure.core/get response 2))
                      (:status response))]
    (<= lower status upper)))

(def ^{:arglists '([response])} success?
  (partial check-status 200 299))

(def ^{:arglists '([response])} client-error?
  (partial check-status 400 499))

(def ^{:arglists '([response])} server-error?
  (partial check-status 500 599))

(defn ^:private handle-ui-error! [ch]
  #?(:clj  ch
     :cljs (async/go
             (let [[_ {:keys [status]} :as result] (async/<! ch)]
               (case status
                 :http.status/unauthorized (nav/go-to! (nav/path-for :auth/logout))
                 result)))))

(def ^:private client*
  (-> http*/request
      client/wrap-query-params
      client/wrap-basic-auth
      client/wrap-oauth
      client/wrap-url
      client/wrap-accept
      client/wrap-content-type
      client/wrap-form-params
      client/wrap-method
      #?@(:clj  [client/wrap-request-timing
                 client/wrap-decompression
                 client/wrap-input-coercion
                 client/wrap-user-info
                 client/wrap-additional-header-parsing
                 client/wrap-output-coercion
                 client/wrap-exceptions
                 client/wrap-nested-params
                 client/wrap-accept-encoding
                 client/wrap-flatten-nested-params
                 client/wrap-unknown-host]
          :cljs [client/wrap-multipart-params
                 client/wrap-channel-from-request-map])))

(defn ^:private client [request]
  #?(:clj  (let [cs (clj-http.cookies/cookie-store)
                 ch (async/chan)]
             (-> request
                 (update :headers (partial maps/map-keys keywords/safe-name))
                 (merge {:async? true :cookie-store cs})
                 (client* (fn [response]
                            (async/put! ch (assoc response :cookies (clj-http.cookies/get-cookies cs))))
                          (fn [exception]
                            (async/put! ch (assoc (ex-data exception) :cookies (clj-http.cookies/get-cookies cs))))))
             ch)
     :cljs (-> request
               (assoc-in [:headers :x-csrf-token] (env/get :csrf-token))
               (update :headers (partial maps/map-keys keywords/safe-name))
               (client*))))

(defn ^:private request* [chan]
  (async/go
    (let [ch-response (async/<! chan)
          {:keys [headers] :as response} (-> (if-let [data (ex-data ch-response)]
                                               data
                                               ch-response)
                                             (update :headers (partial maps/map-keys keyword)))
          serde (case (:content-type headers)
                  "application/transit+json" transit/serde
                  "application/edn" edn/serde
                  "application/json" json/serde
                  nil)]
      (-> response
          (update :status #(status->kw % %))
          (update :body #(cond->> %
                           (and serde (string? %)) (serde/deserialize serde)))
          (->> (conj [(if (success? response) :success :error)]))))))

(defn ^:private go [method url {:keys [response?] :as request}]
  (assert url "Cannot make http request without specifying URI")
  (let [content-type (if (env/get :dev?) "application/edn" "application/transit+json")
        headers (merge {:content-type content-type :accept content-type}
                       (:headers request))]
    (-> request
        (assoc :method method :url url)
        (content/prepare header-keys (:content-type headers))
        (update :headers merge headers)
        (client)
        (request*)
        (handle-ui-error!)
        (cond->
          (not response?) (ch/then :body)))))

(defn get
  ([url]
   (get url nil))
  ([url request]
   (go :get url request)))

(defn post [url request]
  (go :post url request))

(defn patch [url request]
  (go :patch url request))

(defn put [url request]
  (go :put url request))

(defn delete
  ([url]
   (delete url nil))
  ([url request]
   (go :delete url request)))
