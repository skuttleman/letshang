(ns com.ben-allred.letshang.common.utils.strings
  (:require
    [clojure.string :as string]))

(defn trim-to-nil [s]
  (when-let [s (and s (string/trim s))]
    (when-not (empty? s)
      s)))

(defn empty-to-nil [s]
  (when (seq s)
    s))

(defn maybe-pr-str [s]
  (cond-> s
    (not (string? s)) (pr-str)))

(defn titlize
  ([s] (titlize s "-"))
  ([s sep]
   (let [[_ trail-dash] (re-find #"[^-]*(-+)$" s)]
     (str (->> (string/split s #"-")
               (map string/capitalize)
               (string/join sep))
          trail-dash))))

(defn commanate [xs]
  (if (<= (count xs) 2)
    (string/join " and " xs)
    (loop [result (first xs) [s & more] (rest xs)]
      (if (empty? more)
        (str result ", and " s)
        (recur (str result ", " s) more)))))
