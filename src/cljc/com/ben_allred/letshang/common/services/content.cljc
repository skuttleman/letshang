(ns com.ben-allred.letshang.common.services.content
  (:require
    #?(:clj [com.ben-allred.letshang.api.services.streams :as streams])
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.common.utils.serde.core :as serde]
    [com.ben-allred.letshang.common.utils.serde.edn :as edn]
    [com.ben-allred.letshang.common.utils.serde.json :as json]
    [com.ben-allred.letshang.common.utils.serde.transit :as transit]))

(defn ^:private content-type->header [content-type]
  (condp re-find (str content-type)
    #"application/edn" "application/edn"
    #"application/transit\+json" "application/transit+json"
    "application/json"))

(defn ^:private content-type->serde [content-type]
  (condp re-find (str content-type)
    #"application/edn" edn/serde
    #"application/json" json/serde
    #"application/transit\+json" transit/serde
    nil))

(defn ^:private with-headers [request header-keys content-type]
  (let [type (content-type->header content-type)]
    (update request :headers (partial merge (zipmap header-keys (repeat type))))))

(defn ^:private when-not-string [body f serde]
  (if (string? body)
    body
    (f serde body)))

(defn ^:private try-to [value f serde]
  (try (f serde value)
       (catch #?(:clj Throwable :cljs :default) _
         nil)))

(defn ^:private blank? [value]
  (or (nil? value)
      (= "" value)))

(defn parse [data content-type]
  (let [serde (content-type->serde content-type)]
    (cond-> data
      (blank? (:body data)) (dissoc data :body)
      serde (maps/update-maybe :body try-to serde/deserialize serde))))

(defn prepare [data header-keys accept]
  (let [serde (content-type->serde accept)]
    (cond-> data
      (blank? (:body data))
      (dissoc :body)

      serde
      (->
        (maps/update-maybe :body when-not-string serde/serialize serde)
        (with-headers header-keys accept))

      (and (not serde)
           #?(:clj (not (streams/input-stream? (:body data))))
           (or (not accept) (re-find #"\*/\*" accept)))
      (->
        (maps/update-maybe :body when-not-string serde/serialize json/serde)
        (with-headers header-keys "application/json")))))
