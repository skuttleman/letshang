(ns com.ben-allred.letshang.common.services.store.core
  (:require
    [com.ben-allred.collaj.core :as collaj]
    [com.ben-allred.collaj.enhancers :as collaj.enhancers]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.services.store.ui-reducers :as reducers]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defonce ^:private store
  (apply collaj/create-custom-store
         r/atom
         reducers/root
         (env/get :initial-state)
         collaj.enhancers/with-fn-dispatch
         collaj.enhancers/with-subscribers
         (cond-> nil
           (env/get :dev?) (conj #?(:cljs (collaj.enhancers/with-log-middleware
                                            (partial js/console.log "Action dispatched:")
                                            (partial js/console.log "New state:")))))))

(defonce ^:dynamic get-state (:get-state store))

(defonce dispatch (:dispatch store))

(defonce subscribe (:subscribe store))

(defonce user (delay (:auth/user (get-state))))

(defn reaction [path]
  (r/make-reaction (fn [] (get-in (get-state) path))))
