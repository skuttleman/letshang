(ns com.ben-allred.letshang.common.services.http
  (:refer-clojure :exclude [get])
  (:require
    [#?(:clj clojure.core.async :cljs cljs.core.async) :as async]
    [clojure.set :as set]
    [com.ben-allred.letshang.common.services.content :as content]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.logging :as log #?@(:cljs [:include-macros true])]
    [kvlt.chan :as kvlt]))

(def ^:private header-keys #{:content-type :accept})

(defn ^:private content-type-header [{:keys [headers]}]
  (clojure.core/get headers "content-type" (:content-type headers)))

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

(def success?
  (partial check-status 200 299))

(def client-error?
  (partial check-status 400 499))

(def server-error?
  (partial check-status 500 599))

(defn request* [chan]
  (async/go
    (let [ch-response (async/<! chan)
          {:keys [status] :as response} (if-let [data (ex-data ch-response)]
                                          data
                                          ch-response)
          body (-> response
                   (content/parse (content-type-header response))
                   (:body))
          status (status->kw status status)]
      (if (success? response)
        [:success body status response]
        [:error body status response]))))

(defn go [method url request]
  (let [content-type (if (env/get :dev?) "application/edn" "application/transit")
        headers (merge {:content-type content-type :accept content-type}
                       (:headers request))]
    (-> request
        (assoc :method method :url url)
        (content/prepare header-keys (:content-type headers))
        (update :headers merge headers)
        (kvlt/request!)
        (request*))))

(defn get [url & [request]]
  (go :get url request))

(defn post [url request]
  (go :post url request))

(defn patch [url request]
  (go :patch url request))

(defn put [url request]
  (go :put url request))

(defn delete [url & [request]]
  (go :delete url request))
