(ns com.ben-allred.letshang.common.utils.serde.json
  (:require
    #?(:clj [jsonista.core :as jsonista])
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.serde.core :as serde]))

(def ^:private mapper
  #?(:clj  (jsonista/object-mapper
             {:encode-key-fn keywords/safe-name
              :decode-key-fn keyword})
     :cljs nil))

(defn decode [value]
  #?(:clj  (jsonista/read-value value mapper)
     :cljs (js->clj (.parse js/JSON value) :keywordize-keys true)))

(defn encode [value]
  #?(:clj  (jsonista/write-value-as-string value)
     :cljs (.stringify js/JSON (clj->js value))))

(def serde
  (reify serde/ISerDe
    (serialize [_ value]
      (encode value))
    (deserialize [_ value]
      (decode value))))
