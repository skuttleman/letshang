(ns com.ben-allred.letshang.integration.utils.helpers
  (:require
    [com.ben-allred.letshang.api.services.db.migrations :as migrations]
    [com.ben-allred.letshang.api.services.navigation :as nav]
    [com.ben-allred.letshang.common.services.http :as http]
    [com.ben-allred.letshang.integration.utils.http :as test.http]
    [com.ben-allred.letshang.common.utils.logging :as log]))

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
