(ns com.ben-allred.letshang.common.services.content
  (:require
    #?(:clj [com.ben-allred.letshang.api.services.streams :as streams])
    [com.ben-allred.letshang.common.utils.encoders.edn :as edn]
    [com.ben-allred.letshang.common.utils.encoders.json :as json]
    [com.ben-allred.letshang.common.utils.encoders.transit :as transit]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]))

(defn ^:private content-type->header [content-type]
  (condp re-find (str content-type)
    #"application/edn" "application/edn"
    #"application/transit" "application/transit"
    "application/json"))

(defn ^:private content-type->encoder [content-type]
  (condp re-find (str content-type)
    #"application/edn" edn/encode
    #"application/json" json/encode
    #"application/transit" transit/encode
    nil))

(defn ^:private content-type->decoder [content-type]
  (condp re-find (str content-type)
    #"application/edn" edn/decode
    #"application/json" json/decode
    #"application/transit" transit/decode
    nil))

(defn ^:private with-headers [request header-keys content-type]
  (let [type (content-type->header content-type)]
    (update request :headers (partial merge (zipmap header-keys (repeat type))))))

(defn ^:private when-not-string [body f]
  (if (string? body)
    body
    (f body)))

(defn ^:private try-to [value f]
  (try (f value)
       (catch #?(:clj Throwable :cljs :default) _
         nil)))

(defn ^:private blank? [value]
  (or (nil? value)
      (= "" value)))

(defn parse [data content-type]
  (let [decode (content-type->decoder content-type)]
    (cond-> data
      (blank? (:body data)) (dissoc data :body)
      decode (maps/update-maybe :body try-to decode))))

(defn prepare [data header-keys accept]
  (let [encode (content-type->encoder accept)]
    (cond-> data
      (blank? (:body data))
      (dissoc :body)

      encode
      (->
        (maps/update-maybe :body when-not-string encode)
        (with-headers header-keys accept))

      (and (not encode)
           #?(:clj (not (streams/input-stream? (:body data))))
           (or (not accept) (re-find #"\*/\*" accept)))
      (->
        (maps/update-maybe :body when-not-string json/encode)
        (with-headers header-keys "application/json")))))
