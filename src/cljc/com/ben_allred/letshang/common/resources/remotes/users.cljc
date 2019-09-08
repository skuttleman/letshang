(ns com.ben-allred.letshang.common.resources.remotes.users
  (:require
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [com.ben-allred.letshang.common.resources.core :as res]
    [com.ben-allred.letshang.common.resources.remotes.impl :as r.impl]
    [com.ben-allred.letshang.common.services.store.actions.users :as act.users]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as vp])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(defn sign-up [user]
  (let [status (r/atom true)]
    (reify
      r.impl/IRemote
      (success? [_] @status)
      (ready? [_] @status)
      (persist! [_ model]
        (reset! status false)
        (-> {:data model}
            (act.users/register-user)
            (store/dispatch)
            (v/peek (fn [_]
                      (nav/go-to! (nav/path-for :auth/login {:query-params (select-keys model #{:email})})))
                    nil)
            (v/peek (fn [_] (reset! status true)))))

      r.impl/ICache
      (invalidate! [this] this)

      vp/IPromise
      (then [_ on-success _]
        (v/resolve (on-success user)))

      IDeref
      #?(:clj  (deref [_] user)
         :cljs (-deref [_] user)))))

(defonce users
  (r.impl/create {:fetch    (constantly act.users/fetch-associates)
                  :reaction (store/reaction [:associates])}))
