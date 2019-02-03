(ns com.ben-allred.letshang.common.stubs.store
  #?(:cljs
     (:require
       [com.ben-allred.letshang.ui.services.store.core :as store.core])))

(def ^:private noop (constantly nil))

(def get-state
  #?(:clj  noop
     :cljs store.core/get-state))

(def dispatch
  #?(:clj  (fn dispatch [action]
             (if (fn? action)
               (action [dispatch])
               nil))
     :cljs store.core/dispatch))
