(ns com.ben-allred.letshang.api.services.streams
  (:require
    [com.ben-allred.letshang.common.utils.logging :as log])
  (:import
    (java.io File InputStream)
    (org.apache.commons.io FileUtils)))

(defn open-input-stream [file]
  (when (instance? File file)
    (FileUtils/openInputStream ^File file)))

(defn file? [file]
  (instance? File file))

(defn input-stream? [value]
  (instance? InputStream value))

(defn delete [file]
  (when (and file (file? file))
    (.delete ^File file)))
