(ns com.ben-allred.letshang.common.resources.core
  (:require
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.services.store.actions.toast :as act.toast]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn toast-error [msg]
  (fn [error]
    (->> (:message error msg)
         (act.toast/toast! :error)
         (store/dispatch))))

(defn toast-success [msg]
  (fn [_]
    (->> msg
         (act.toast/toast! :success)
         (store/dispatch))))
