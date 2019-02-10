(ns com.ben-allred.letshang.common.utils.encoders.jwt
  (:require
    #?(:clj [clj-jwt.core :as clj-jwt])
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.utils.encoders.transit :as transit])
  #?(:clj
     (:import
       (java.util Date)
       (org.joda.time DateTime))))

(def ^:private jwt-secret (env/get :jwt-secret))

(defn ^:private decode* [token]
  #?(:clj  (try
             (clj-jwt/str->jwt token)
             (catch Throwable e
               nil))
     :cljs (throw (js/Error. "Not Implemented"))))

(defn valid? [token]
  #?(:clj  (if-let [decoded (decode* token)]
             (clj-jwt/verify decoded)
             false)
     :cljs (throw (js/Error. "Not Implemented"))))

(defn decode [token]
  (some-> token
          (decode*)
          (:claims)
          (update :data transit/decode)))

(defn encode
  ([payload]
   (encode payload 30))
  ([payload days-to-expire]
    #?(:clj  (let [now (dates/now)]
               (-> {:iat  (-> now
                              (Date/from)
                              (.getTime)
                              (DateTime.))
                    :data (-> payload
                              (transit/encode))
                    :exp  (-> now
                              (dates/plus days-to-expire :days)
                              (Date/from)
                              (.getTime)
                              (DateTime.))}
                   (clj-jwt/jwt)
                   (clj-jwt/sign :HS256 jwt-secret)
                   (clj-jwt/to-str)))
       :cljs (throw (js/Error. "Not Implemented")))))
