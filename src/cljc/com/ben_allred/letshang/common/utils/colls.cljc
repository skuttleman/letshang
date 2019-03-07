(ns com.ben-allred.letshang.common.utils.colls
  (:refer-clojure :exclude [find]))

(defn cons? [coll]
  (and (sequential? coll)
       (not (vector? coll))))

(defn force-sequential [v]
  (if (or (nil? v) (sequential? v))
    v
    [v]))

(defn force-vector [v]
  (if (vector? v)
    v
    [v]))

(defn assoc-by [compare-fn value coll]
  (let [comparator (compare-fn value)
        replaced? (volatile! false)]
    (-> coll
        (->>
          (map (fn [old-value]
                 (if (= comparator (compare-fn old-value))
                   (do (vreset! replaced? true)
                       value)
                   old-value)))
          (doall))
        (cond->
          (not @replaced?) (concat [value])))))

(defn only! [[item & more]]
  (assert (empty? more) "Should only be one item")
  item)

(defn prepend
  ([x]
   (fn [rf]
     (let [prepended? (volatile! false)]
       (fn
         ([]
          (rf (rf) x))
         ([result]
          (if @prepended?
            (rf result)
            (rf (rf result x))))
         ([result input]
          (if @prepended?
            (rf result input)
            (do (vreset! prepended? true)
                (rf (rf result x) input))))))))
  ([coll x]
   (if (vector? coll)
     (into [x] coll)
     (cons x coll))))

(defn append
  ([x]
   (fn [rf]
     (fn
       ([]
        (rf))
       ([result]
        (rf (unreduced (rf result x))))
       ([result input]
        (rf result input)))))
  ([coll x]
   (if (vector? coll)
     (conj coll x)
     (concat coll [x]))))

(defn supdate [coll seq-fn f & f-args]
  (seq-fn #(apply f % f-args) coll))

(defn find [pred coll]
  (first (filter pred coll)))
