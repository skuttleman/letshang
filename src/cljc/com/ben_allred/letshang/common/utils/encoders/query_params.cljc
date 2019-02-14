(ns com.ben-allred.letshang.common.utils.encoders.query-params
  (:require
    [clojure.string :as string]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]))

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
  [[key (str (keywords/safe-name value))]])

(defn encode [qp]
  (->> qp
       (mapcat encodify)
       (map (partial string/join "="))
       (string/join "&")))

(defn decode [s]
  )
