(ns com.ben-allred.letshang.common.services.navigation
  (:require
    [bidi.bidi :as bidi]
    [clojure.string :as string]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.encoders.query-params :as qp]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]))

(def app-routes
  [""
   [;; AUTH
    ["/auth"
     [["/callback" :auth/callback]
      ["/login" :auth/login]
      ["/logout" :auth/logout]
      ["/register" :auth/register]]]

    ;; API
    ["/api"
     [["/hangouts"
       [["" :api/hangouts]
        [["/" [uuids/regex :hangout-id]] :api/hangout]
        [["/" [uuids/regex :hangout-id] "/suggestions"]
         [["/when" :api/suggestion.when]
          ["/where" :api/suggestion.where]]]]]
      ["/invitations"
       [[["/" [uuids/regex :invitation-id]] :api/invitation]]]
      ["/users"
       [["/associates" :api/associates]]]]]

    ;; UI
    ["/" :ui/home]
    ["/hangouts"
     [["" :ui/hangouts]
      ["/new" :ui/hangout-new]
      [["/" [uuids/regex :hangout-id]] :ui/hangout]]]
    [true :ui/not-found]]])

(defn ^:private namify [[k v]]
  [k (str (keywords/safe-name v))])

(defn ^:private re-format [{:keys [handler] :as route}]
  (cond-> route
    (#{:api/hangout :ui/hangout} handler) (update-in [:route-params :hangout-id] uuids/->uuid)))

(defn match-route [routes path]
  (let [qp (qp/decode (second (string/split path #"\?")))]
    (-> routes
        (bidi/match-route path)
        (cond-> (seq qp) (assoc :query-params qp))
        (re-format))))

(defn path-for [routes page {:keys [query-params route-params]}]
  (let [qp (qp/encode query-params)]
    (cond-> (apply bidi/path-for routes page (mapcat namify route-params))
      (seq query-params) (str "?" qp))))
