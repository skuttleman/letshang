(ns com.ben-allred.letshang.common.utils.serde.query-params
  (:require
    [clojure.string :as string]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.serde.core :as serde])
  #?(:clj
     (:import
       (java.net URLEncoder))))

(defn ^:private encode-type [_ value]
  (cond
    (map? value) :map
    (coll? value) :coll
    (boolean? value) :bool))

(defmulti ^:private encode* #'encode-type)

(defn ^:private encodify [[key value]]
  (encode* (str (keywords/safe-name key)) value))

(defn ^:private str-map [map-key outer-key]
  (let [[_ pre post] (re-matches #"([^\[]+)(.*)" map-key)]
    (str outer-key "[" pre "]" post)))

(defn ^:private str-coll [key]
  (let [[_ pre post] (re-matches #"([^\[]+)(.*)" key)]
    (str pre "[]" post)))

(defmethod  ^:private encode* :map
  [key value]
  (->> value
       (mapcat encodify)
       (map #(update % 0 str-map key))))

(defmethod ^:private encode* :coll
  [key value]
  (->> value
       (mapcat #(encode* key %))
       (map #(update % 0 str-coll))))

(defmethod ^:private encode* :bool
  [key value]
  (when value
    [[key]]))

(defmethod ^:private encode* :default
  [key value]
  (when (some? value)
    [[key (-> value
              (keywords/safe-name)
              (str)
              #?(:clj (URLEncoder/encode) :cljs (js/encodeURIComponent)))]]))

(defn ^:private decodify [k v]
  (cond
    (seq v) [[(keyword k) v]]
    :else [[(keyword k) true]]))

(defn encode [qp]
  (->> qp
       (mapcat encodify)
       (map (partial string/join "="))
       (string/join "&")))

(defn decode [s]
  (->> (string/split s #"&")
       (map (comp vec #(string/split % #"=")))
       (reduce (fn [qp [k v]] (into qp (decodify k v))) {})))

(def serde
  (reify serde/ISerDe
    (serialize [_ value]
      (encode value))
    (deserialize [_ value]
      (decode value))))
