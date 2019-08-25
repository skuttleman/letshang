(ns com.ben-allred.letshang.common.utils.serde.edn
  (:require
    #?(:clj  [com.ben-allred.letshang.api.services.streams :as streams]
       :cljs [java.time :refer [LocalDate LocalDateTime]])
    [#?(:clj clojure.edn :cljs cljs.reader) :as edn*]
    [cljc.java-time.local-date :as ld]
    [clojure.pprint :as pp]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.serde.core :as serde]
    [com.ben-allred.letshang.common.utils.strings :as strings]
    [cljc.java-time.local-date-time :as ldt])
  #?(:clj
     (:import
       (java.io Writer)
       (java.time LocalDate LocalDateTime))))

#?(:clj
   (do
     (defmethod print-method LocalDate [x writer]
       (.write ^Writer writer ^String (strings/format "#inst/date \"%s\"" (ld/to-string x))))

     (defmethod print-method LocalDateTime [x writer]
       (.write ^Writer writer ^String (strings/format "#inst/datetime \"%s\"" (ldt/to-string x)))))
   :cljs
   (do
     (extend-type LocalDate
       IPrintWithWriter
       (-pr-writer [x writer _]
         (-write writer (strings/format "#inst/date \"%s\"" (ld/to-string x)))))

     (extend-type LocalDateTime
       IPrintWithWriter
       (-pr-writer [x writer _]
         (-write writer (strings/format "#inst/datetime \"%s\"" (ldt/to-string x)))))))

(defn ^:private maybe-slurp [value]
  #?(:clj  (if (streams/input-stream? value)
             (slurp value)
             value)
     :cljs value))

(def ^{:arglists '([value])} decode (comp (partial edn*/read-string {:readers {'inst/date ld/parse}}) maybe-slurp))

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
