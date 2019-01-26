(ns com.ben-allred.letshang.common.services.content
  (:require
    [#?(:clj clojure.edn :cljs cljs.reader) :as edn]
    [com.ben-allred.letshang.common.utils.json :as json]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.common.utils.transit :as transit]
    #?(:clj [com.ben-allred.letshang.api.services.streams :as streams]))
  #?(:clj
     (:import
       (java.io InputStream))))

(defn ^:private with-headers [request header-keys type]
  (update request :headers (partial merge (zipmap header-keys (repeat type)))))

(defn ^:private maybe-slurp [value]
  (if #?(:clj (streams/input-stream? value) :cljs true)
    #?(:clj (slurp value))
    value))

(defn ^:private when-not-string [body f]
  (if (string? body)
    body
    (f body)))

(defn ^:private try-to [value f]
  (try (f value)
       (catch #?(:clj Throwable :cljs js/Object) _
         nil)))

(def ^:private edn?
  (comp (partial re-find #"application/edn") str))

(def ^:private json?
  (comp (partial re-find #"application/json") str))

(def ^:private transit?
  (comp (partial re-find #"application/transit") str))

(defn parse [data content-type]
  (cond-> data
    (= "" (:body data)) (dissoc data :body)
    (edn? content-type) (maps/update-maybe :body try-to (comp edn/read-string maybe-slurp))
    (json? content-type) (maps/update-maybe :body try-to json/parse)
    (transit? content-type) (maps/update-maybe :body try-to transit/parse)))

(defn prepare [data header-keys accept]
  (cond-> data
    (= "" (:body data))
    (dissoc :body)

    (edn? accept)
    (->
      (maps/update-maybe :body when-not-string pr-str)
      (with-headers header-keys "application/edn"))

    (transit? accept)
    (->
      (maps/update-maybe :body when-not-string transit/stringify)
      (with-headers header-keys "application/transit"))

    (and #?(:clj (not (streams/input-stream? (:body data))))
         (or (not accept) (re-find #"\*/\*" accept) (json? accept)))
    (->
      (maps/update-maybe :body when-not-string json/stringify)
      (with-headers header-keys "application/json"))))
