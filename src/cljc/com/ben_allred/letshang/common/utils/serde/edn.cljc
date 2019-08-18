(ns com.ben-allred.letshang.common.utils.serde.edn
  (:require
    #?(:clj [com.ben-allred.letshang.api.services.streams :as streams])
    [#?(:clj clojure.edn :cljs cljs.reader) :as edn*]
    [clojure.pprint :as pp]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.serde.core :as serde]))

(defn ^:private maybe-slurp [value]
  #?(:clj  (if (streams/input-stream? value)
             (slurp value)
             value)
     :cljs value))

(def ^{:arglists '([value])} decode (comp edn*/read-string maybe-slurp))

(defn encode [value]
  (if (env/get :dev?)
    (with-out-str (pp/pprint value))
    (pr-str value)))

(def serde
  (reify serde/ISerDe
    (serialize [_ value]
      (encode value))
    (deserialize [_ value]
      (decode value))))
