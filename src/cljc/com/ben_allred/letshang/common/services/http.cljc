(ns com.ben-allred.letshang.common.services.http
  (:refer-clojure :exclude [get])
  (:require
    [#?(:clj clj-http.client :cljs cljs-http.client) :as client]
    [#?(:clj clojure.core.async :cljs cljs.core.async) :as async]
    [clojure.set :as set]
    [com.ben-allred.letshang.common.services.content :as content]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.encoders.edn :as edn]
    [com.ben-allred.letshang.common.utils.encoders.json :as json]
    [com.ben-allred.letshang.common.utils.encoders.transit :as transit]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log #?@(:cljs [:include-macros true])]
    [com.ben-allred.letshang.common.utils.maps :as maps]))

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

(defn ^:private client [request]
  #?(:clj  (let [cs (clj-http.cookies/cookie-store)
                 ch (async/chan)]
             (-> request
                 (update :headers (partial maps/map-keys keywords/safe-name))
                 (merge {:async? true :cookie-store cs})
                 (client/request (fn [response]
                                   (async/put! ch (assoc response :cookies (clj-http.cookies/get-cookies cs))))
                                 (fn [exception]
                                   (async/put! ch (assoc (ex-data exception) :cookies (clj-http.cookies/get-cookies cs))))))
             ch)
     :cljs (-> request
               (assoc-in [:headers :x-csrf-token] (env/get :csrf-token))
               (update :headers (partial maps/map-keys keywords/safe-name))
               (client/request))))

(defn ^:private request* [chan]
  (async/go
    (let [ch-response (async/<! chan)
          {:keys [body headers status] :as response} (-> (if-let [data (ex-data ch-response)]
                                                   data
                                                   ch-response)
                                                 (update :headers (partial maps/map-keys keyword)))
          status (status->kw status status)
          body (case (when (string? body) (:content-type headers))
                 "application/transit" (transit/decode body)
                 "application/edn" (edn/decode body)
                 "application/json" (json/decode body)
                 body)]
      (if (success? response)
        [:success body status response]
        [:error body status response]))))

(defn ^:private go [method url request]
  (let [content-type (if (env/get :dev?) "application/edn" "application/transit")
        headers (merge {:content-type content-type :accept content-type}
                       (:headers request))]
    (-> request
        (assoc :method method :url url)
        (content/prepare header-keys (:content-type headers))
        (update :headers merge headers)
        (client)
        (request*))))

(defn get
  ([url]
   (get url nil))
  ([url & [request]]
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
