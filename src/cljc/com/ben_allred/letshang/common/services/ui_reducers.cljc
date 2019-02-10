(ns com.ben-allred.letshang.common.services.ui-reducers
  (:require
    [com.ben-allred.collaj.reducers :as collaj.reducers]
    [com.ben-allred.letshang.common.utils.logging :as log #?@(:cljs [:include-macros true])]
    [com.ben-allred.letshang.common.utils.maps :as maps #?@(:cljs [:include-macros true])]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]))

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
(def ^:private hangout (resource :hangout))
(def ^:private hangouts (resource :hangouts))

(defn ^:private page
  ([] nil)
  ([state [type page]]
   (case type
     :router/navigate page
     state)))

(def root
  (collaj.reducers/combine (maps/->map associates
                                       hangout
                                       hangouts
                                       page)))
