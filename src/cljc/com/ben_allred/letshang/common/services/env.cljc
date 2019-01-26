(ns com.ben-allred.letshang.common.services.env
  (:refer-clojure :exclude [get])
  #?(:clj
     (:require
       [environ.core :as environ])))

(def get
  #?(:clj  environ/env
     :cljs (-> {:host     (.-host (.-location js/window))
                :protocol (if (re-find #"https" (.-protocol (.-location js/window)))
                            :https
                            :http)}
               (merge (js->clj (aget js/window "ENV") :keywordize-keys true)))))
