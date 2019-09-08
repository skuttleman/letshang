(ns com.ben-allred.letshang.common.resources.remotes.moments
  (:require
    [com.ben-allred.letshang.common.resources.remotes.impl :as r.impl]
    [com.ben-allred.letshang.common.services.store.actions.hangouts :as act.hangouts]
    [com.ben-allred.letshang.common.services.store.core :as store]))

(defonce moments
  (let [hangout-id (store/reaction [:page :route-params :hangout-id])]
    (r.impl/create {:fetch #(act.hangouts/fetch-moments @hangout-id)
                    :reaction (store/reaction [:moments])
                    :invalidate! (constantly [:moments/invalidate!])})))
