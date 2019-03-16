(ns com.ben-allred.letshang.api.services.streams
  (:require
    [com.ben-allred.letshang.common.utils.logging :as log])
  (:import
    (java.io File InputStream)))

(defn file? [file]
  (instance? File file))

(defn input-stream? [value]
  (instance? InputStream value))
