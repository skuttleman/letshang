(ns com.ben-allred.letshang.common.services.navigation
  (:require
    [bidi.bidi :as bidi]
    [clojure.string :as string]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.encoders.query-params :as qp]
    [com.ben-allred.letshang.common.utils.fns #?(:clj :refer :cljs :refer-macros) [=>]]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]
    [com.ben-allred.letshang.common.utils.maps :as maps]))

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
        [["/" [uuids/regex :hangout-id]]
         [["" :api/hangout]
          ["/invitations" :api/hangout.invitations]
          ["/locations" :api/hangout.locations]
          ["/moments" :api/hangout.moments]]]]]
      ["/invitations"
       [[["/" [uuids/regex :invitation-id]] :api/invitation]]]
      ["/locations"
       [[["/" [uuids/regex :location-id]] :api/location]]]
      ["/moments"
       [[["/" [uuids/regex :moment-id]] :api/moment]]]
      ["/users"
       [["/associates" :api/associates]]]]]

    ;; UI
    ["/" :ui/home]
    ["/hangouts"
     [["" :ui/hangouts]
      ["/new" :ui/hangouts.new]
      [["/" [uuids/regex :hangout-id] "/" [#"invitations|locations|moments" :section]] :ui/hangout]]]
    [true :ui/not-found]]])

(defn ^:private namify [[k v]]
  [k (str (keywords/safe-name v))])

(defn ^:private re-format [route]
  (-> route
      (maps/update-maybe :route-params (=> (maps/update-maybe :hangout-id uuids/->uuid)
                                           (maps/update-maybe :section keyword)))))

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
