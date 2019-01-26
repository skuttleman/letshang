(ns com.ben-allred.letshang.common.utils.keywords
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
