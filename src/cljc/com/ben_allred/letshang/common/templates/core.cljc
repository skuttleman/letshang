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

(defn ^:private clean-attrs [attrs]
  (-> attrs
      (maps/dissocp (some-fn nil? fn?))
      (maps/walk (fn [k v] [k (if (keyword? v) (name v) v)]))
      (set/rename-keys {:class-name :class})
      (maps/update-maybe :style m->css)))

(defn ^:private render* [arg]
  (cond
    (vector? arg) (render arg)
    (or (seq? arg) (list? arg)) (map render arg)
    (map? arg) (clean-attrs arg)
    :else arg))

(defn classes
  ([rules] (classes nil rules))
  ([attrs rules]
   (let [classes (->> rules
                      (filter val)
                      (map (comp name key))
                      (string/join " "))]
     (cond-> attrs
       (seq classes) (update :class-name (comp strings/trim-to-nil str) " " classes)))))

(defn render [[node & args :as tree]]
  (when tree
    (let [[node & args] (if (fn? node)
                          (loop [node (apply node args)]
                            (if (fn? node)
                              (recur (apply node args))
                              (render node)))
                          tree)]
      (into [node] (map render*) args))))
