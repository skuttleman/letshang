(ns com.ben-allred.letshang.ui.services.store.actions
  (:require
    [com.ben-allred.letshang.common.services.http :as http]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.ui.services.navigation :as nav]
    [com.ben-allred.letshang.common.utils.logging :as log]))

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

(defn update-hangout [hangout-id hangout]
  (fn [[dispatch]]
    (-> (nav/path-for :api/hangout {:route-params {:hangout-id hangout-id}})
        (http/patch {:body hangout})
        (request* dispatch :hangout))))
