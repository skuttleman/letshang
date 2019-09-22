(ns com.ben-allred.letshang.common.resources.remotes.locations
  (:require
    [com.ben-allred.letshang.common.resources.remotes.impl :as r.impl]
    [com.ben-allred.letshang.common.services.store.actions.hangouts :as act.hangouts]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defonce locations
  (let [hangout-id (store/reaction [:page :route-params :hangout-id])
        locations (store/reaction [:locations])]
    (r.impl/create {:fetch      #(act.hangouts/fetch-locations @hangout-id)
                    :reaction   locations})))
