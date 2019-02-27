(ns com.ben-allred.letshang.common.utils.encoders.edn
  (:require
    #?(:clj [com.ben-allred.letshang.api.services.streams :as streams])
    [#?(:clj clojure.edn :cljs cljs.reader) :as edn*]
    [com.ben-allred.letshang.common.services.env :as env]
    [clojure.pprint :as pp]))

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
