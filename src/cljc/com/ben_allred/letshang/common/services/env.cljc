(ns com.ben-allred.letshang.common.services.env
  (:refer-clojure :exclude [get])
  (:require
    #?(:clj [environ.core :as environ])
    [com.ben-allred.letshang.common.utils.dom :as dom]
    [com.ben-allred.letshang.common.utils.encoders.transit :as transit]))

(defonce get
  #?(:clj  environ/env
     :cljs (-> {:host     (.-host (.-location dom/window))
                :protocol (if (re-find #"https" (.-protocol (.-location dom/window)))
                            :https
                            :http)}
               (merge (transit/decode (aget dom/window "ENV"))))))
