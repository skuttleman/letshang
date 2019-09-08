(ns com.ben-allred.letshang.common.services.store.actions.toast
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.letshang.common.utils.dates :as dates]))


(defn remove-toast! [toast-id]
  (fn [[dispatch]]
    #?@(:cljs
        [(dispatch [:toast/hide {:id toast-id}])
         (async/go
           (async/<! (async/timeout 500))
           (dispatch [:toast/remove {:id toast-id}]))])
    nil))

(defn toast! [level body]
  (fn [[dispatch]]
    #?(:cljs (let [toast-id (dates/inst->ms (dates/now))]
               (->> {:id    toast-id
                     :level level
                     :body  (delay (async/go
                                     (dispatch [:toast/show {:id toast-id}])
                                     (async/<! (async/timeout 6000))
                                     (dispatch (remove-toast! toast-id)))
                                   body)}
                    (conj [:toast/add])
                    (dispatch))))
    nil))
