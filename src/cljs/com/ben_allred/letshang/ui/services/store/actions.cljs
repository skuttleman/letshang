(ns com.ben-allred.letshang.ui.services.store.actions
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.letshang.common.services.http :as http]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.ui.services.navigation :as nav]))

(defn ^:private request* [request dispatch kwd-ns]
  (dispatch [(keywords/namespaced kwd-ns :request)])
  (ch/peek request
           (fn [[status result]]
             (dispatch [(keywords/namespaced kwd-ns status) result]))))

(defn combine [& actions]
  (fn [[dispatch]]
    (->> actions
         (map dispatch)
         (doall)
         (ch/all))))

(defn create-hangout [hangout]
  (fn [[dispatch]]
    (-> (nav/path-for :api/hangouts)
        (http/post {:body hangout})
        (request* dispatch :hangout.new))))

(defn fetch-associates [[dispatch]]
  (-> (nav/path-for :api/associates)
      (http/get)
      (request* dispatch :associates)))

(defn fetch-hangout [hangout-id]
  (fn [[dispatch]]
    (-> (nav/path-for :api/hangout {:route-params {:hangout-id hangout-id}})
        (http/get)
        (request* dispatch :hangout))))

(defn fetch-hangouts [[dispatch]]
  (-> (nav/path-for :api/hangouts)
      (http/get)
      (request* dispatch :hangouts)))

(defn remove-toast [toast-id]
  (fn [[dispatch]]
    (dispatch [:toast/hide {:id toast-id}])
    (async/go
      (async/<! (async/timeout 500))
      (dispatch [:toast/remove {:id toast-id}]))
    nil))

(defn set-response [invitation-id body]
  (fn [[dispatch]]
    (-> (nav/path-for :api/invitation {:route-params {:invitation-id invitation-id}})
        (http/patch {:body body})
        (request* dispatch :invitations))))

(defn toast [level body]
  (fn [[dispatch]]
    (let [toast-id (dates/inst->ms (dates/now))]
      (->> {:id    toast-id
            :level level
            :body  (delay (async/go
                            (dispatch [:toast/show {:id toast-id}])
                            (async/<! (async/timeout 6000))
                            (dispatch (remove-toast toast-id)))
                          body)}
           (conj [:toast/add])
           (dispatch))
      nil)))

(defn update-hangout [hangout-id hangout]
  (fn [[dispatch]]
    (-> (nav/path-for :api/hangout {:route-params {:hangout-id hangout-id}})
        (http/patch {:body hangout})
        (request* dispatch :hangout))))
