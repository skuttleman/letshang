(ns com.ben-allred.letshang.integration.utils.http
  (:refer-clojure :exclude [get])
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.services.http :as http]
    [com.ben-allred.letshang.common.utils.fns :as fns]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn request* [f [auth-token csrf-token] path request assert?]
  (let [response (-> request
                     (update [:headers "accept"] fns/or "application/transit+json")
                     (cond->
                       auth-token (assoc-in [:headers "cookie"] (str "auth-token=" auth-token))
                       csrf-token (assoc-in [:headers "x-csrf-token"] csrf-token))
                     (->> (f (str (env/get :base-url) path)))
                     (deref))]
    (when assert? (assert (http/success? response) (pr-str (second response))))
    response))

(defn get [path auth-token & [request]]
  (request* http/get auth-token path request false))

(defn patch [path auth-token & [request]]
  (request* http/patch auth-token path request false))

(defn post [path auth-token & [request]]
  (request* http/post auth-token path request false))

(defn put [path auth-token & [request]]
  (request* http/put auth-token path request false))

(defn get! [path auth-token & [request]]
  (second (request* http/get auth-token path request true)))

(defn patch! [path auth-token & [request]]
  (second (request* http/patch auth-token path request true)))

(defn post! [path auth-token & [request]]
  (second (request* http/post auth-token path request true)))

(defn put! [path auth-token & [request]]
  (second (request* http/put auth-token path request true)))
