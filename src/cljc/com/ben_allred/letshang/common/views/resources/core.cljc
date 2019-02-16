(ns com.ben-allred.letshang.common.views.resources.core
  (:require
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.stubs.store :as store]
    [com.ben-allred.letshang.common.stubs.actions :as actions]))

(defn toast-error [error]
  (->> (:message error "Something went wrong.")
       (actions/toast :error)
       (store/dispatch)))
