(ns com.ben-allred.letshang.common.utils.keywords
  (:refer-clojure :exclude [replace str])
  (:require
    [clojure.string :as string]))

(defn str [v]
  (if (keyword? v)
    (subs (clojure.core/str v) 1)
    v))

(defn safe-name [v]
  (if (keyword? v)
    (name v)
    v))

(defn join
  ([kwds] (join "" kwds))
  ([separator kwds]
   (->> kwds
        (map (comp clojure.core/str safe-name))
        (string/join (safe-name separator))
        (keyword))))

(defn namespaced [ns name']
  (keyword (name ns) (name name')))

(defn replace [kw re replacement]
  (-> kw
      (safe-name)
      (string/replace re (safe-name replacement))
      (keyword)))

(defn snake->kebab [kw]
  (replace kw #"_" :-))

(defn kebab->snake [kw]
  (replace kw #"-" :_))
