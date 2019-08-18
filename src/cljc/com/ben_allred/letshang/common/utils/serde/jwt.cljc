(ns com.ben-allred.letshang.common.utils.serde.jwt
  (:require
    #?(:clj [clj-jwt.core :as clj-jwt])
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.utils.serde.core :as serde]
    [com.ben-allred.letshang.common.utils.serde.transit :as transit])
  #?(:clj
     (:import
       (java.util Date)
       (org.joda.time DateTime))))

(defn ^:private decode* [token]
  #?(:clj  (try
             (clj-jwt/str->jwt token)
             (catch Throwable _
               nil))
     :cljs (throw (js/Error. "Not Implemented"))))

(defn decode [value]
  (some-> value
          (decode*)
          (:claims)
          (update :data transit/decode)))

(defn encode [payload]
  #?(:clj  (let [days-to-expire (env/get :jwt-expiration 30)
                 jwt-secret (env/get :jwt-secret)
                 now (dates/now)]
             (-> {:iat  (-> now
                            (dates/inst->ms)
                            (DateTime.))
                  :data (transit/encode payload)
                  :exp  (-> now
                            (dates/plus days-to-expire :days)
                            (dates/inst->ms)
                            (DateTime.))}
                 (clj-jwt/jwt)
                 (clj-jwt/sign :HS256 jwt-secret)
                 (clj-jwt/to-str)))
     :cljs (throw (js/Error. "Not Implemented"))))

(def serde
  (reify serde/ISerDe
    (serialize [_ value]
      (encode value))
    (deserialize [_ value]
      (decode value))))
