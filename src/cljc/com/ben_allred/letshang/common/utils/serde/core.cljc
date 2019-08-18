(ns com.ben-allred.letshang.common.utils.serde.core
  #?(:clj (:gen-class)))

(defprotocol ISerDe
  (serialize [this value])
  (deserialize [this value]))
