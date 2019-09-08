(ns com.ben-allred.letshang.common.services.store.actions.shared
  (:require
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.vow.core :as v]))

(defn request [request dispatch kwd-ns]
  (dispatch [(keywords/namespaced kwd-ns :request)])
  (v/peek request
          (fn [[status result]]
            (dispatch [(keywords/namespaced kwd-ns status) result]))))

