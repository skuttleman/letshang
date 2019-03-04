(ns com.ben-allred.letshang.common.services.ui-reducers
  (:require
    [com.ben-allred.collaj.reducers :as collaj.reducers]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log #?@(:cljs [:include-macros true])]
    [com.ben-allred.letshang.common.utils.maps :as maps #?@(:cljs [:include-macros true])]))

(defn ^:private resource [namespace]
  (let [[request success error] (->> [:request :success :error]
                                     (map (partial keywords/namespaced namespace)))]
    (fn
      ([] [:init])
      ([state [type response]]
       (condp = type
         request [:requesting]
         success [:success (:data response)]
         error [:error response]
         state)))))

(def ^:private associates (resource :associates))
(def ^:private hangouts (resource :hangouts))
(def ^:private hangout (collaj.reducers/comp
                         (fn
                           ([] [:init])
                           ([state [type response]]
                            (case type
                              :suggestions.when/success (update-in state [1 :moments] conj (:data response))
                              state)))
                         (resource :hangout)))

(defn ^:private page
  ([] nil)
  ([state [type page]]
   (case type
     :router/navigate page
     state)))

(defn ^:private toasts
  ([] {})
  ([state [type {:keys [id level body]}]]
   (case type
     :toast/add (assoc state id {:state :init :level level :body body})
     :toast/show (maps/update-maybe state id assoc :state :showing)
     :toast/hide (maps/update-maybe state id assoc :state :removing)
     :toast/remove (dissoc state id)
     state)))

(def root
  (collaj.reducers/combine (maps/->map associates
                                       hangout
                                       hangouts
                                       page
                                       toasts)))
