(ns com.ben-allred.letshang.common.services.navigation
  (:require
    [bidi.bidi :as bidi]
    [clojure.string :as string]
    [com.ben-allred.letshang.common.utils.fns #?(:clj :refer :cljs :refer-macros) [=>]]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.common.utils.serde.query-params :as qp]
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
     [["/events" :api/events]
      ["/hangouts"
       [["" :api/hangouts]
        [["/" [uuids/regex :hangout-id]]
         [["" :api/hangout]
          ["/invitations" :api/hangout.invitations]
          ["/locations" :api/hangout.locations]
          ["/messages" :api/hangout.messages]
          ["/moments" :api/hangout.moments]]]]]
      ["/invitations"
       [[["/" [uuids/regex :invitation-id]]
         [["" :api/invitation]
          ["/responses" :api/invitation.responses]]]]]
      ["/locations"
       [[["/" [uuids/regex :location-id]]
         [["" :api/location]
          ["/responses" :api/location.responses]]]]]
      ["/moments"
       [[["/" [uuids/regex :moment-id]]
         [["" :api/moment]
          ["/responses" :api/moment.responses]]]]]
      ["/users"
       [["/associates" :api/associates]]]
      [true :api/not-found]]]

    ;; UI
    ["/" :ui/home]
    ["/hangouts"
     [["" :ui/hangouts]
      ["/new" :ui/hangouts.new]
      [["/" [uuids/regex :hangout-id] "/" [#"conversation|invitations|locations|moments" :section]] :ui/hangout]]]

    ;; RESOURCES
    ["/health" :resources/health]
    ["/js" [[true :resources/js]]]
    ["/css" [[true :resources/css]]]
    ["/images" [[true :resources/images]]]
    ["/favicon.ico" :resources/favicon]
    [true :nav/not-found]]])

(defn ^:private namify [[k v]]
  [k (str (keywords/safe-name v))])

(defn ^:private re-format [route]
  (-> route
      (maps/update-maybe :route-params (=> (maps/update-maybe :hangout-id uuids/->uuid)
                                           (maps/update-maybe :section keyword)))
      (maps/update-in-maybe [:query-params :uri] qp/decode-param)))

(defn match-route [routes path]
  (let [qp (some->> (string/split path #"\?") (second) (qp/decode))]
    (-> routes
        (bidi/match-route path)
        (cond-> (seq qp) (assoc :query-params qp))
        (re-format))))

(defn path-for [routes page {:keys [query-params route-params]}]
  (let [qp (qp/encode query-params)]
    (cond-> (apply bidi/path-for routes page (mapcat namify route-params))
      (seq qp) (str "?" qp))))
