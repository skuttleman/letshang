(ns com.ben-allred.letshang.common.services.store.actions.hangouts
  (:require
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [com.ben-allred.letshang.common.services.http :as http]
    [com.ben-allred.letshang.common.services.store.actions.shared :as act]
    [com.ben-allred.letshang.common.utils.chans :as ch]))

(defn create-hangout [hangout]
  (fn [[dispatch]]
    #?(:clj  (ch/resolve)
       :cljs (-> (nav/path-for :api/hangouts)
                 (http/post {:body hangout})
                 (act/request dispatch :hangout.new)))))

(defn fetch-hangout [hangout-id]
  (fn [[dispatch]]
    #?(:clj  (ch/resolve)
       :cljs (-> (nav/path-for :api/hangout {:route-params {:hangout-id hangout-id}})
                 (http/get)
                 (act/request dispatch :hangout)))))

(defn fetch-hangouts [[dispatch]]
  #?(:clj  (ch/resolve)
     :cljs (-> (nav/path-for :api/hangouts)
               (http/get)
               (act/request dispatch :hangouts))))

(defn update-hangout [hangout-id hangout]
  (fn [[dispatch]]
    #?(:clj  (ch/resolve)
       :cljs (-> (nav/path-for :api/hangout {:route-params {:hangout-id hangout-id}})
                 (http/patch {:body hangout})
                 (act/request dispatch :hangout)))))

(defn set-response [response-type id body]
  (fn [[dispatch]]
    #?(:clj  (ch/resolve)
       :cljs (let [[route param kwd-ns] (case response-type
                                          :invitation [:api/invitation :invitation-id :invitations]
                                          :moment [:api/moment :moment-id :moment]
                                          :location [:api/location :location-id :location])]
               (-> (nav/path-for route {:route-params {param id}})
                   (http/patch {:body body})
                   (act/request dispatch kwd-ns))))))

(defn suggest-who [hangout-id suggestion]
  (fn [[dispatch]]
    #?(:clj  (ch/resolve)
       :cljs (-> (nav/path-for :api/suggestions.who {:route-params {:hangout-id hangout-id}})
                 (http/post {:body suggestion})
                 (act/request dispatch :suggestions.who)))))

(defn suggest-when [hangout-id suggestion]
  (fn [[dispatch]]
    #?(:clj  (ch/resolve)
       :cljs (-> (nav/path-for :api/suggestions.when {:route-params {:hangout-id hangout-id}})
                 (http/post {:body suggestion})
                 (act/request dispatch :suggestions.when)))))

(defn suggest-where [hangout-id suggestion]
  (fn [[dispatch]]
    #?(:clj  (ch/resolve)
       :cljs (-> (nav/path-for :api/suggestions.where {:route-params {:hangout-id hangout-id}})
                 (http/post {:body suggestion})
                 (act/request dispatch :suggestions.where)))))
