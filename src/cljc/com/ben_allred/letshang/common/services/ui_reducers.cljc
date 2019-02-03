(ns com.ben-allred.letshang.common.services.ui-reducers
  (:require
    [com.ben-allred.collaj.reducers :as collaj.reducers]
    [com.ben-allred.letshang.common.utils.logging :as log #?@(:cljs [:include-macros true])]
    [com.ben-allred.letshang.common.utils.maps :as maps #?@(:cljs [:include-macros true])]))

(defn page
  ([] nil)
  ([state [type page]]
   (case type
     :router/navigate page
     state)))

(defn hangouts
  ([] [:init])
  ([state [type response]]
   (case type
     :hangouts/request [:requesting]
     :hangouts/success [:success (:data response)]
     :hangouts/error [:error response]
     state)))

(defn hangout
  ([] [:init])
  ([state [type response]]
   (case type
     :hangout/request [:requesting]
     :hangout/success [:success (:data response)]
     :hangout/error [:error response]
     state)))

(def root
  (collaj.reducers/combine (maps/->map hangout
                                       hangouts
                                       page)))
