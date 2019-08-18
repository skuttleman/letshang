(ns com.ben-allred.letshang.common.utils.serde.core)

(defprotocol ISerDe
  (serialize [this value])
  (deserialize [this value]))
