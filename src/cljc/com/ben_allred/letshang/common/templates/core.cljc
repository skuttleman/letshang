(ns com.ben-allred.letshang.common.templates.core
  (:require
    [clojure.set :as set]
    [clojure.string :as string]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.common.utils.strings :as strings]))

(declare render)

(defn ^:private m->css [m]
  (if (map? m)
    (->> m
         (map (fn [[k v]] (str (name k) ": " v)))
         (string/join ";"))
    m))

(defn ^:private coll->class [class]
  (if (string? class)
    class
    (string/join " " (filter some? class))))

(defn ^:private clean-attrs [attrs]
  (-> attrs
      (maps/dissocp (some-fn nil? fn?))
      (maps/walk (fn [k v] [k (if (keyword? v) (name v) v)]))
      (maps/update-maybe :class coll->class)
      (maps/update-maybe :style m->css)))

(defn ^:private render* [arg]
  (cond
    (vector? arg) (render arg)
    (or (seq? arg) (list? arg)) (map render arg)
    (map? arg) (clean-attrs arg)
    :else arg))

(defn render [[node & args :as tree]]
  (when tree
    (let [[node & args] (if (fn? node)
                          (loop [node (apply node args)]
                            (if (fn? node)
                              (recur (apply node args))
                              (render node)))
                          tree)]
      (into [node] (map render*) args))))
