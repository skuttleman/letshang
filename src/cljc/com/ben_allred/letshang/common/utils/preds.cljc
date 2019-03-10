(ns com.ben-allred.letshang.common.utils.preds
  #?(:clj  (:import
             (java.util.regex Pattern))
     :cljs (:refer-clojure :exclude [regexp?])))

(def regexp?
  #?(:clj  (partial instance? Pattern)
     :cljs cljs.core/regexp?))
