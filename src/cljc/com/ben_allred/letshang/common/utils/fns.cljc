(ns com.ben-allred.letshang.common.utils.fns
  (:refer-clojure :exclude [and or]))

(defmacro => [& forms]
  `(fn [arg#]
     (-> arg# ~@forms)))

(defmacro =>> [& forms]
  `(fn [arg#]
     (->> arg# ~@forms)))

(defn and [& values]
  (loop [[val & more] values]
    (if (empty? more)
      val
      (clojure.core/and val (recur more)))))

(defn or [& values]
  (loop [[val & more] values]
    (if (empty? more)
      val
      (clojure.core/or val (recur more)))))

(defn each!
  ([f!]
   (fn [rf]
     (fn
       ([] (rf))
       ([result] (rf result))
       ([result input]
        (f! input)
        (rf result input)))))
  ([f! coll]
   (map (comp first (juxt identity f!)) coll)))

(defn intov [xform]
  (fn [coll]
    (transduce xform conj coll)))
