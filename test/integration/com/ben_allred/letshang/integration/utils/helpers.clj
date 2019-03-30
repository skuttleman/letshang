(ns com.ben-allred.letshang.integration.utils.helpers
  (:require
    [clojure.core.async :as async]
    [clojure.core.async.impl.protocols :as async.protocols]
    [clojure.string :as string]
    [com.ben-allred.letshang.api.services.db.migrations :as migrations]
    [com.ben-allred.letshang.api.services.navigation :as nav]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.services.http :as http]
    [com.ben-allred.letshang.common.utils.encoders.transit :as transit]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.integration.utils.http :as test.http]
    [gniazdo.core :as gniazdo])
  (:import
    (java.net URI)))

(defn with-timeout
  ([chan]
   (with-timeout chan 100))
  ([chan ms]
   (async/go
     (let [[value] (async/alts! [chan (async/go
                                        (async/<! (async/timeout ms))
                                        ::timed-out)])]
       (when (not= ::timed-out value)
         value)))))

(defn ^:private http* [request]
  (-> request
      (second)
      (:data)))

(defn login [email]
  (-> (test.http/request* http/get nil (nav/path-for :auth/login {:query-params {:email email}}) nil false)
      (get-in [3 :cookies "auth-token" :value])))

(defn seed! [seed-file]
  (migrations/seed! (str "db/test/" seed-file)))

(defn fetch-associates [token]
  (-> (nav/path-for :api/associates)
      (test.http/get token)
      (http*)))

(defn fetch-hangouts [token]
  (-> (nav/path-for :api/hangouts)
      (test.http/get token)
      (http*)))

(defn fetch-hangout [token hangout-id]
  (-> (nav/path-for :api/hangout {:route-params {:hangout-id hangout-id}})
      (test.http/get token)
      (http*)))

(defn fetch-invitations [token hangout-id]
  (-> (nav/path-for :api/hangout.invitations {:route-params {:hangout-id hangout-id}})
      (test.http/get token)
      (http*)))

(defn fetch-locations [token hangout-id]
  (-> (nav/path-for :api/hangout.locations {:route-params {:hangout-id hangout-id}})
      (test.http/get token)
      (http*)))

(defn fetch-moments [token hangout-id]
  (-> (nav/path-for :api/hangout.moments {:route-params {:hangout-id hangout-id}})
      (test.http/get token)
      (http*)))

(defn create-hangout [token data]
  (-> (nav/path-for :api/hangouts)
      (test.http/post token {:body {:data data}})
      (http*)))

(defn create-message [token hangout-id data]
  (-> (nav/path-for :api/hangout.messages {:route-params {:hangout-id hangout-id}})
      (test.http/post token {:body {:data data}})
      (http*)))

(defn update-hangout [token hangout-id data]
  (-> (nav/path-for :api/hangout {:route-params {:hangout-id hangout-id}})
      (test.http/patch token {:body {:data data}})
      (http*)))

(defn suggest-who [token hangout-id user-ids]
  (-> (nav/path-for :api/hangout.invitations {:route-params {:hangout-id hangout-id}})
      (test.http/post token {:body {:data {:invitation-ids user-ids}}})
      (http*)))

(defn suggest-when [token hangout-id data]
  (-> (nav/path-for :api/hangout.moments {:route-params {:hangout-id hangout-id}})
      (test.http/post token {:body {:data data}})
      (http*)))

(defn suggest-where [token hangout-id data]
  (-> (nav/path-for :api/hangout.locations {:route-params {:hangout-id hangout-id}})
      (test.http/post token {:body {:data data}})
      (http*)))

(defn ws-connect [auth-token]
  (let [url (-> (env/get :base-url)
                (string/replace #"http" "ws")
                (str (nav/path-for :api/events)))
        uri (URI. url)
        ch (async/chan 64)
        open-ch (async/chan)
        client (doto (gniazdo/client uri)
                 (.start))
        ws (gniazdo/connect url
                            :client client
                            :headers {"cookie" (str "auth-token=" auth-token)}
                            :on-receive (fn [s]
                                          (let [msg (transit/decode s)]
                                            (when (not= [:ws/ping] msg)
                                              (async/put! ch msg))))
                            :on-connect (partial async/put! open-ch))]
    (async/<!! open-ch)
    (async/close! open-ch)
    (reify
      async.protocols/ReadPort
      (take! [_ handler]
        (async.protocols/take! (with-timeout ch) handler))

      async.protocols/WritePort
      (put! [_ msg _]
        (gniazdo/send-msg ws (transit/encode msg))
        (future
          (Thread/sleep 10)
          true))

      async.protocols/Channel
      (close! [this]
        (when-not (async.protocols/closed? this)
          (.stop client)
          (gniazdo/close ws)))
      (closed? [_]
        (and (not (.isRunning client))
             (not (.isStarted client))
             (not (.isStarting client)))))))
