(ns com.ben-allred.letshang.common.resources.remotes.core
  (:require
    [com.ben-allred.letshang.common.resources.remotes.impl :as r.impl]))

(defn success? [remote]
  (r.impl/success? remote))

(defn ready? [remote]
  (r.impl/ready? remote))

(defn invalidate! [remote]
  (r.impl/invalidate! remote))

(defn persist! [remote model]
  (r.impl/persist! remote model))
