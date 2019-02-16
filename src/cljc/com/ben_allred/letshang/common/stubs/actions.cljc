(ns com.ben-allred.letshang.common.stubs.actions
  (:require
    #?(:cljs
       [com.ben-allred.letshang.ui.services.store.actions :as store.actions])
    [com.ben-allred.letshang.common.utils.chans :as ch]))

(def ^:private noop ch/resolve)

(def combine
  #?(:clj  (constantly noop)
     :cljs store.actions/combine))

(defn create-hangout [hangout]
  #?(:clj  noop
     :cljs (store.actions/create-hangout hangout)))

(def fetch-associates
  #?(:clj  noop
     :cljs store.actions/fetch-associates))

(def fetch-hangouts
  #?(:clj  noop
     :cljs store.actions/fetch-hangouts))

(defn fetch-hangout [hangout-id]
  #?(:clj  noop
     :cljs (store.actions/fetch-hangout hangout-id)))

(defn remove-toast [toast-id]
  #?(:clj  noop
     :cljs (store.actions/remove-toast toast-id)))

(defn toast [level body]
  #?(:clj  noop
     :cljs (store.actions/toast level body)))

(defn update-hangout [hangout-id hangout]
  #?(:clj  noop
     :cljs (store.actions/update-hangout hangout-id hangout)))
