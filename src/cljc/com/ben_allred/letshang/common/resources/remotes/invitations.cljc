(ns com.ben-allred.letshang.common.resources.remotes.invitations
  (:require
    [com.ben-allred.letshang.common.resources.remotes.impl :as r.impl]
    [com.ben-allred.letshang.common.services.store.actions.hangouts :as act.hangouts]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.colls :as colls]))

(defonce invitations
  (let [hangout-id (store/reaction [:page :route-params :hangout-id])
        invitations (store/reaction [:invitations])]
    (r.impl/create {:fetch      #(act.hangouts/fetch-invitations @hangout-id)
                    :reaction   invitations})))
