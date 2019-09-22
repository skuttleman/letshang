(ns com.ben-allred.letshang.common.resources.remotes.suggestions
  (:refer-clojure :exclude [when])
  (:require
    [com.ben-allred.letshang.common.resources.remotes.impl :as r.impl]
    [com.ben-allred.letshang.common.resources.remotes.invitations :as rem.invitations]
    [com.ben-allred.letshang.common.resources.remotes.locations :as rem.locations]
    [com.ben-allred.letshang.common.resources.remotes.moments :as rem.moments]
    [com.ben-allred.letshang.common.resources.remotes.users :as rem.users]
    [com.ben-allred.letshang.common.services.store.actions.hangouts :as act.hangouts]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defonce who
  (let [reactions {:associates  rem.users/users
                   :invitations rem.invitations/invitations}
        reaction (r/make-reaction (fn []
                                    (let [reactions (vals reactions)]
                                      (cond
                                        (every? r.impl/success? reactions)
                                        [:success nil]

                                        (every? r.impl/ready? reactions)
                                        (->> reactions
                                             (remove r.impl/success?)
                                             (first)
                                             (deref))

                                        :else
                                        [:init]))))
        hangout-id (store/reaction [:page :route-params :hangout-id])]
    (r.impl/create {:reaction reaction
                    :persist  (fn [model]
                                (act.hangouts/suggest :who @hangout-id {:data model}))})))

(defonce when
  (let [reactions {:moment rem.moments/moments}
        reaction (r/make-reaction (fn []
                                    (let [reactions (vals reactions)]
                                      (cond
                                        (every? r.impl/success? reactions)
                                        [:success {:window :any-time}]

                                        (every? r.impl/ready? reactions)
                                        (->> reactions
                                             (remove r.impl/success?)
                                             (first)
                                             (deref))

                                        :else
                                        [:init]))))
        hangout-id (store/reaction [:page :route-params :hangout-id])]
    (r.impl/create {:reaction reaction
                    :persist  (fn [model]
                                (act.hangouts/suggest :when @hangout-id {:data model}))})))

(defonce where
  (let [reactions {:locations rem.locations/locations}
        reaction (r/make-reaction (fn []
                                    (let [reactions (vals reactions)]
                                      (cond
                                        (every? r.impl/success? reactions)
                                        [:success nil]

                                        (every? r.impl/ready? reactions)
                                        (->> reactions
                                             (remove r.impl/success?)
                                             (first)
                                             (deref))

                                        :else
                                        [:init]))))
        hangout-id (store/reaction [:page :route-params :hangout-id])]
    (r.impl/create {:reaction reaction
                    :persist  (fn [model]
                                (act.hangouts/suggest :where @hangout-id {:data model}))})))
