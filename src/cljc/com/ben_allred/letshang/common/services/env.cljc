(ns com.ben-allred.letshang.common.services.env
  (:refer-clojure :exclude [get])
  (:require
    #?(:clj  [environ.core :as environ]
       :cljs [com.ben-allred.letshang.common.utils.transit :as transit])))

(def get
  #?(:clj  environ/env
     :cljs (-> {:host     (.-host (.-location js/window))
                :protocol (if (re-find #"https" (.-protocol (.-location js/window)))
                            :https
                            :http)}
               (merge (transit/parse (aget js/window "ENV"))))))
