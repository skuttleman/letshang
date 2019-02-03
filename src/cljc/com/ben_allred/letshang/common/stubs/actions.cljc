(ns com.ben-allred.letshang.common.stubs.actions
  (:require
    #?(:cljs
       [com.ben-allred.letshang.ui.services.store.actions :as store.actions])
    [com.ben-allred.letshang.common.utils.chans :as ch]))

(def ^:private noop ch/resolve)

(def fetch-hangouts
  #?(:clj  noop
     :cljs store.actions/fetch-hangouts))

(defn fetch-hangout [hangout-id]
  #?(:clj  noop
     :cljs (store.actions/fetch-hangout hangout-id)))
