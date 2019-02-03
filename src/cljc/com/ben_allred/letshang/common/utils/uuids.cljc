(ns com.ben-allred.letshang.common.utils.uuids
  #?(:clj
     (:import
       (java.util UUID))))

(def regex #"(?i)[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")

(defn uuid-str? [s]
  (and (string? s)
       (boolean (re-matches regex s))))

(defn ->uuid [v]
  (when v
    (if (uuid? v)
      v
      #?(:clj  (UUID/fromString v)
         :cljs (uuid v)))))

(defn random []
  #?(:clj  (UUID/randomUUID)
     :cljs (random-uuid)))
