(ns com.ben-allred.letshang.integration.utils.helpers
  (:require
    [clj-jwt.base64 :as base64]
    [clojure.core.async :as async]
    [clojure.string :as string]
    [com.ben-allred.letshang.api.services.db.migrations :as migrations]
    [com.ben-allred.letshang.api.services.navigation :as nav]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.services.http :as http]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.serde.edn :as edn]
    [com.ben-allred.letshang.common.utils.serde.json :as json]
    [com.ben-allred.letshang.common.utils.serde.transit :as transit]
    [com.ben-allred.letshang.integration.utils.http :as test.http]
    [ws-client-cljc.core :as ws*]))

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
  (let [token (-> (test.http/request* http/get
                                      nil
                                      (nav/path-for :auth/login {:query-params {:email email}})
                                      {:response? true}
                                      false)
                  (get-in [1 :cookies "auth-token" :value]))
        user-id (-> token
                    (string/split #"\.")
                    (second)
                    (base64/url-safe-decode-str)
                    (json/decode)
                    (:data)
                    (transit/decode)
                    (get-in [:user :id]))]
    (-> (test.http/request* http/get
                            [user-id token]
                            (nav/path-for :ui/home)
                            {:headers {"accept" "text/html"}}
                            false)
        (second)
        (->> (re-find #"window.ENV=(.+);"))
        (second)
        (edn/decode)
        (transit/decode)
        (:csrf-token)
        (->> (conj [user-id token])))))

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
      (test.http/put token {:body {:data {:invitation-ids user-ids}}})
      (http*)))

(defn suggest-when [token hangout-id data]
  (-> (nav/path-for :api/hangout.moments {:route-params {:hangout-id hangout-id}})
      (test.http/put token {:body {:data data}})
      (http*)))

(defn suggest-where [token hangout-id data]
  (-> (nav/path-for :api/hangout.locations {:route-params {:hangout-id hangout-id}})
      (test.http/put token {:body {:data data}})
      (http*)))

(defn respond-who [token invitation-id response]
  (-> (nav/path-for :api/invitation.responses {:route-params {:invitation-id invitation-id}})
      (test.http/put token {:body {:data {:response response}}})
      (http*)))

(defn respond-when [token moment-id response]
  (-> (nav/path-for :api/moment.responses {:route-params {:moment-id moment-id}})
      (test.http/put token {:body {:data {:response response}}})
      (http*)))

(defn respond-where [token location-id response]
  (-> (nav/path-for :api/location.responses {:route-params {:location-id location-id}})
      (test.http/put token {:body {:data {:response response}}})
      (http*)))

(defn ws-connect [[_ auth-token csrf-token]]
  (let [url (-> (env/get :base-url)
                (string/replace #"http" "ws")
                (str (nav/path-for :api/events {:query-params {:x-csrf-token csrf-token
                                                               :auth-token   auth-token}})))]
    (ws*/connect! url {:in-buf-or-n  100
                       :out-buf-or-n 100
                       :in-xform     (comp (map transit/decode)
                                           (remove #{[:ws/ping]}))
                       :out-xform    (map transit/encode)})))
