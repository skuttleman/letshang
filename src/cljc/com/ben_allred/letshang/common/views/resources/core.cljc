(ns com.ben-allred.letshang.common.views.resources.core
  (:require
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.stubs.store :as store]
    [com.ben-allred.letshang.common.stubs.actions :as actions]))

(defn toast-error [msg]
  (fn [error]
    (some->> (:message error msg)
             (actions/toast :error)
             (store/dispatch))))

(defn toast-success [msg]
  (fn [_]
    (->> msg
         (actions/toast :success)
         (store/dispatch))))
