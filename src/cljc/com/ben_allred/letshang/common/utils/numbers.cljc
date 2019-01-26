(ns com.ben-allred.letshang.common.utils.numbers
  (:refer-clojure :exclude [number?]))

(defn nan? [value]
  #?(:clj  (and (clojure.core/number? value)
                (Double/isNaN (double value)))
     :cljs (js/isNaN value)))

(defn number? [value]
  (and (clojure.core/number? value) (not (nan? value))))

(defn parse-int [value]
  #?(:clj  (try (Long/parseLong (str value))
                (catch Throwable ex
                  Double/NaN))
     :cljs (js/parseInt value)))
