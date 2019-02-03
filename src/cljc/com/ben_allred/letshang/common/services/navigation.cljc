(ns com.ben-allred.letshang.common.services.navigation
  (:require
    [bidi.bidi :as bidi]
    [clojure.string :as string]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.common.utils.query-params :as qp]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]))

(def app-routes
  [""
   [;; AUTH
    ["/auth"
     [["/callback" :auth/callback]
      ["/login" :auth/login]
      ["/logout" :auth/logout]]]

    ;; API
    ["/api"
     [["/hangouts"
       [["" :api/hangouts]
        [["/" [uuids/regex :hangout-id]] :api/hangout]]]]]

    ;; UI
    ["/" :ui/home]
    ["/hangouts"
     [["" :ui/hangouts]
      ["/new" :ui/hangout-new]
      [["/" [uuids/regex :hangout-id]] :ui/hangout]]]
    [true :ui/not-found]]])

(defn ^:private namify [[k v]]
  [k (str (keywords/safe-name v))])

(defn ^:private re-format [route]
  (-> route
      (maps/update-maybe :route-params maps/update-maybe :hangout-id uuids/->uuid)))

(defn match-route [routes path]
  (let [qp (qp/parse (second (string/split path #"\?")))]
    (-> routes
        (bidi/match-route path)
        (re-format)
        (cond-> (seq qp) (assoc :query-params qp)))))

(defn path-for [routes page {:keys [query-params route-params]}]
  (let [qp (qp/stringify query-params)]
    (cond-> (apply bidi/path-for routes page (mapcat namify route-params))
      (seq query-params) (str "?" qp))))
