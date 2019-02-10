(ns com.ben-allred.letshang.common.services.env
  (:refer-clojure :exclude [get])
  (:require
    #?(:clj  [environ.core :as environ]
       :cljs [com.ben-allred.letshang.common.utils.transit :as transit])
    [com.ben-allred.letshang.common.utils.dom :as dom]))

(def get
  #?(:clj  environ/env
     :cljs (-> {:host     (.-host (.-location dom/window))
                :protocol (if (re-find #"https" (.-protocol (.-location dom/window)))
                            :https
                            :http)}
               (merge (transit/parse (aget dom/window "ENV"))))))
