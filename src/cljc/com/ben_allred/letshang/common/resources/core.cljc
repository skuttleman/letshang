(ns com.ben-allred.letshang.common.resources.core
  (:require
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.services.store.actions :as actions]))

(defn toast-error [msg]
  (fn [error]
    (->> (:message error msg)
         (actions/toast :error)
         (store/dispatch))))

(defn toast-success [msg]
  (fn [_]
    (->> msg
         (actions/toast :success)
         (store/dispatch))))
