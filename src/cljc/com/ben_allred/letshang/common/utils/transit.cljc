(ns com.ben-allred.letshang.common.utils.transit
  (:require
    [cognitect.transit :as trans]
    [com.ben-allred.letshang.common.utils.logging :as log])
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

(defn parse [s]
  #?(:clj  (if (instance? InputStream s)
             (trans/read (trans/reader s :json))
             (trans/read (trans/reader (string->stream s) :json)))
     :cljs (trans/read reader s)))

(defn stringify [v]
  #?(:clj  (let [out (ByteArrayOutputStream. 4096)]
             (trans/write (trans/writer out :json) v)
             (.toString out))
     :cljs (trans/write writer v)))
