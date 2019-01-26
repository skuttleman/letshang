(ns com.ben-allred.letshang.common.utils.json
  #?(:clj
     (:require
       [com.ben-allred.letshang.common.utils.keywords :as keywords]
       [com.ben-allred.letshang.common.utils.logging :as log]
       [jsonista.core :as jsonista])))

(def ^:private mapper
  #?(:clj  (jsonista/object-mapper
             {:encode-key-fn keywords/safe-name
              :decode-key-fn keyword})
     :cljs nil))

(defn parse [s]
  #?(:clj  (jsonista/read-value s mapper)
     :cljs (js->clj (.parse js/JSON s) :keywordize-keys true)))

(defn stringify [o]
  #?(:clj  (jsonista/write-value-as-string o)
     :cljs (.stringify js/JSON (clj->js o))))
