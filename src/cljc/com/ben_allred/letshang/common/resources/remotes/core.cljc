(ns com.ben-allred.letshang.common.resources.remotes.core
  (:require
    [com.ben-allred.letshang.common.resources.remotes.impl :as r.impl]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn success? [remote]
  (r.impl/success? remote))

(defn ready? [remote]
  (r.impl/ready? remote))

(defn persist! [remote model]
  (r.impl/persist! remote model))

(defn fetch! [remote]
  (r.impl/fetch! remote))

(defn hydrated? [remote]
  (r.impl/hydrated? remote))
