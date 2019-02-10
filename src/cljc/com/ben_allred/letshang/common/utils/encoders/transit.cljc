(ns com.ben-allred.letshang.common.utils.encoders.transit
  (:require
    [cognitect.transit :as trans])
  #?(:clj
     (:import
       (java.io ByteArrayInputStream ByteArrayOutputStream InputStream))))

(defn ^:private string->stream [s]
  #?(:clj  (-> s
               (.getBytes)
               (ByteArrayInputStream.))
     :cljs nil))

(def ^:private reader
  #?(:clj  nil
     :cljs (trans/reader :json)))

(def ^:private writer
  #?(:clj  nil
     :cljs (trans/writer :json)))

(defn decode [value]
  #?(:clj  (if (instance? InputStream value)
             (trans/read (trans/reader value :json))
             (trans/read (trans/reader (string->stream value) :json)))
     :cljs (trans/read reader value)))

(defn encode [value]
  #?(:clj  (let [out (ByteArrayOutputStream. 4096)]
             (trans/write (trans/writer out :json) value)
             (.toString out))
     :cljs (trans/write writer value)))
