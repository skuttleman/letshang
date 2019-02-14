(ns com.ben-allred.letshang.common.utils.keywords
  (:refer-clojure :exclude [replace])
  (:require
    [clojure.string :as string]))

(defn safe-name [v]
  (if (keyword? v)
    (name v)
    v))

(defn join
  ([kwds] (join "" kwds))
  ([separator kwds]
   (->> kwds
        (map (comp str safe-name))
        (string/join (safe-name separator))
        (keyword))))

(defn namespaced [ns name']
  (keyword (name ns) (name name')))

(defn replace [kw re replacement]
  (-> kw
      (safe-name)
      (string/replace re (safe-name replacement))
      (keyword)))
