(ns com.ben-allred.letshang.common.services.store.actions.hangouts
  (:require
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [com.ben-allred.letshang.common.services.http :as http]
    [com.ben-allred.letshang.common.services.store.actions.shared :as act]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.vow.core :as v]))

(defn create-hangout [hangout]
  (fn [[dispatch]]
    #?(:clj  (v/resolve)
       :cljs (-> (nav/path-for :api/hangouts)
                 (http/post {:body hangout})
                 (act/request dispatch :hangout.new)))))

(defn fetch-hangout [hangout-id]
  (fn [[dispatch]]
    #?(:clj  (v/resolve)
       :cljs (-> (nav/path-for :api/hangout {:route-params {:hangout-id hangout-id}})
                 (http/get)
                 (act/request dispatch :hangout)))))

(defn fetch-hangouts [[dispatch]]
  #?(:clj  (v/resolve)
     :cljs (-> (nav/path-for :api/hangouts)
               (http/get)
               (act/request dispatch :hangouts))))

(defn fetch-invitations [hangout-id]
  (fn [[dispatch]]
    #?(:clj  (v/resolve)
       :cljs (-> (nav/path-for :api/hangout.invitations {:route-params {:hangout-id hangout-id}})
                 (http/get)
                 (act/request dispatch :invitations)))))

(defn fetch-locations [hangout-id]
  (fn [[dispatch]]
    #?(:clj  (v/resolve)
       :cljs (-> (nav/path-for :api/hangout.locations {:route-params {:hangout-id hangout-id}})
                 (http/get)
                 (act/request dispatch :locations)))))

(defn fetch-messages [hangout-id length]
  (fn [[dispatch]]
    #?(:clj  (v/resolve)
       :cljs (-> (nav/path-for :api/hangout.messages {:route-params {:hangout-id hangout-id}
                                                      :query-params {:offset length}})
                 (http/get)
                 (act/request dispatch :messages)))))

(defn fetch-moments [hangout-id]
  (fn [[dispatch]]
    #?(:clj  (v/resolve)
       :cljs (-> (nav/path-for :api/hangout.moments {:route-params {:hangout-id hangout-id}})
                 (http/get)
                 (act/request dispatch :moments)))))

(defn save-message [hangout-id body]
  (fn [[dispatch]]
    #?(:clj  (v/resolve)
       :cljs (-> (nav/path-for :api/hangout.messages {:route-params {:hangout-id hangout-id}})
                 (http/post {:body body})
                 (act/request dispatch :messages.create)))))

(defn set-response [response-type id body]
  (fn [[dispatch]]
    #?(:clj  (v/resolve)
       :cljs (let [[route param kwd-ns] (case response-type
                                          :invitation [:api/invitation.responses :invitation-id :response.invitation]
                                          :moment [:api/moment.responses :moment-id :response.moment]
                                          :location [:api/location.responses :location-id :response.location])]
               (-> (nav/path-for route {:route-params {param id}})
                   (http/put {:body body})
                   (act/request dispatch kwd-ns))))))

(defn suggest [suggestion-type hangout-id suggestion]
  (fn [[dispatch]]
    #?(:clj  (v/resolve)
       :cljs (let [[route kwd-ns] (case suggestion-type
                                    :who [:api/hangout.invitations :suggestions.who]
                                    :when [:api/hangout.moments :suggestions.when]
                                    :where [:api/hangout.locations :suggestions.where])]
               (-> (nav/path-for route {:route-params {:hangout-id hangout-id}})
                   (http/put {:body suggestion})
                   (act/request dispatch kwd-ns))))))

(defn update-hangout [hangout-id hangout]
  (fn [[dispatch]]
    #?(:clj  (v/resolve)
       :cljs (-> (nav/path-for :api/hangout {:route-params {:hangout-id hangout-id}})
                 (http/patch {:body hangout})
                 (act/request dispatch :hangout)))))
