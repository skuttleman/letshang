(ns com.ben-allred.letshang.common.services.store.actions.shared
  (:require
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.chans :as ch]))

(defn request [request dispatch kwd-ns]
  (dispatch [(keywords/namespaced kwd-ns :request)])
  (ch/peek request
           (fn [[status result]]
             (dispatch [(keywords/namespaced kwd-ns status) result]))))

(defn combine [& actions]
  (fn [[dispatch]]
    #?(:clj  (ch/resolve)
       :cljs (->> actions
                  (map dispatch)
                  (doall)
                  (ch/all)))))
