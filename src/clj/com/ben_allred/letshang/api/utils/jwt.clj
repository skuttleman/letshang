(ns com.ben-allred.letshang.api.utils.jwt
  (:require
    [clj-jwt.core :as clj-jwt]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [clj-time.coerce :as time.coerce]
    [com.ben-allred.letshang.common.utils.transit :as transit])
  (:import (java.util Date)))

(def ^:private jwt-secret (env/get :jwt-secret))

(defn ^:private decode* [token]
  (try
    (clj-jwt/str->jwt token)
    (catch Throwable e
      nil)))

(defn valid? [token]
  (if-let [decoded (decode* token)]
    (clj-jwt/verify decoded)
    false))

(defn decode [token]
  (some-> token
          (decode*)
          (:claims)))

(defn encode
  ([payload]
   (encode payload 30))
  ([payload days-to-expire]
   (let [now (dates/now)]
     (-> {:iat  (-> now
                    (Date/from)
                    (time.coerce/from-date))
          :data (-> payload
                    (transit/stringify))
          :exp  (-> now
                    (dates/plus days-to-expire :days)
                    (Date/from)
                    (time.coerce/from-date))}
         (clj-jwt/jwt)
         (clj-jwt/sign :HS256 jwt-secret)
         (clj-jwt/to-str)))))
