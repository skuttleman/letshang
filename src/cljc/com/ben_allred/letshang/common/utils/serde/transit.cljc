(ns com.ben-allred.letshang.common.utils.serde.transit
  (:require
    #?(:cljs [java.time :refer [LocalDate LocalDateTime]])
    [cljc.java-time.local-date :as ld]
    [cljc.java-time.local-date-time :as ldt]
    [cognitect.transit :as trans]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.serde.core :as serde])
  #?(:clj
     (:import
       (clojure.lang IFn)
       (java.io ByteArrayInputStream ByteArrayOutputStream InputStream)
       (java.time LocalDate LocalDateTime))))

(defn ^:private string->stream [s]
  #?(:clj  (cond-> s
             (not (instance? InputStream s)) (-> (.getBytes)
                                                 (ByteArrayInputStream.)))
     :cljs s))

(defn decode [value]
  #?(:clj  (-> value
               (string->stream)
               (trans/reader :json {:handlers {"inst/date"     (trans/read-handler #(ld/parse %))
                                               "inst/datetime" (trans/read-handler #(ldt/parse %))}})
               (trans/read))
     :cljs (-> (trans/reader :json {:handlers {"inst/date"     (trans/read-handler #(ld/parse %))
                                               "inst/datetime" (trans/read-handler #(ldt/parse %))}})
               (trans/read value))))

(defn encode [value]
  #?(:clj  (let [out (ByteArrayOutputStream. 4096)]
             (-> out
                 (trans/writer :json {:handlers {LocalDate     (trans/write-handler (constantly "inst/date") #(ld/to-string %))
                                                 LocalDateTime (trans/write-handler (constantly "inst/datetime") #(ldt/to-string %))}})
                 (trans/write value))
             (.toString out))
     :cljs (-> (trans/writer :json {:handlers {LocalDate     (trans/write-handler (constantly "inst/date") #(ld/to-string %))
                                               LocalDateTime (trans/write-handler (constantly "inst/datetime") #(ldt/to-string %))}})
               (trans/write value))))

(def serde
  (reify serde/ISerDe
    (serialize [_ value]
      (encode value))
    (deserialize [_ value]
      (decode value))))
