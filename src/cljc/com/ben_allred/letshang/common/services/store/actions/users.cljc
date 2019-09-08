(ns com.ben-allred.letshang.common.services.store.actions.users
  (:require
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [com.ben-allred.letshang.common.services.http :as http]
    [com.ben-allred.letshang.common.services.store.actions.shared :as act]
    [com.ben-allred.vow.core :as v]))

(defn fetch-associates [[dispatch]]
  #?(:clj  (v/resolve)
     :cljs (-> (nav/path-for :api/associates)
               (http/get)
               (act/request dispatch :associates))))

(defn register-user [user]
  (fn [[dispatch]]
    #?(:clj  (v/resolve)
       :cljs (-> (nav/path-for :auth/register)
                 (http/post {:body user})
                 (act/request dispatch :user.new)))))
