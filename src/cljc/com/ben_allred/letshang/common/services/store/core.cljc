(ns com.ben-allred.letshang.common.services.store.core
  (:require
    [com.ben-allred.collaj.core :as collaj]
    [com.ben-allred.collaj.enhancers :as collaj.enhancers]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.services.ui-reducers :as reducers]
    [com.ben-allred.letshang.common.stubs.reagent :as r]))

(defonce ^:private store
  (apply collaj/create-custom-store
         r/atom
         reducers/root
         collaj.enhancers/with-fn-dispatch
         (cond-> nil
           (env/get :dev?) (conj #?(:cljs (collaj.enhancers/with-log-middleware
                                            (partial js/console.log "Action dispatched:")
                                            (partial js/console.log "New state:")))))))

(def get-state (:get-state store))

(def dispatch (:dispatch store))
