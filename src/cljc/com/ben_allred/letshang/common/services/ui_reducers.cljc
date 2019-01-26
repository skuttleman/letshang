(ns com.ben-allred.letshang.common.services.ui-reducers
  (:require
    [com.ben-allred.collaj.reducers :as collaj.reducers]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps #?@(:cljs [:include-macros true])]))

(defn page
  ([] nil)
  ([state [type page]]
   (case type
     :router/navigate page
     state)))

(def root
  (collaj.reducers/combine (maps/->map page)))
