(ns com.ben-allred.letshang.common.resources.remotes.responses
  (:require
    [com.ben-allred.letshang.common.resources.remotes.impl :as r.impl]
    [com.ben-allred.letshang.common.resources.remotes.invitations :as rem.invitations]
    [com.ben-allred.letshang.common.resources.remotes.locations :as rem.locations]
    [com.ben-allred.letshang.common.resources.remotes.moments :as rem.moments]
    [com.ben-allred.letshang.common.services.store.actions.hangouts :as act.hangouts]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn ^:private response* [response-type model-id]
  (let [remote (case response-type
                 :moment rem.moments/moments
                 :location rem.locations/locations
                 :invitation rem.invitations/invitations)]
    (r.impl/create {:reaction   (r/make-reaction (fn []
                                                   (cond-> @remote
                                                     (r.impl/success? remote)
                                                     (->
                                                       (cond->
                                                         (not= :invitation response-type)
                                                         (->> (filter (comp #{model-id} :id))
                                                              (first)
                                                              (:responses)))
                                                       (->> (filter (comp #{(:id @store/user)} :user-id)))
                                                       (first)
                                                       (select-keys #{:user-id :response})
                                                       (assoc :id model-id)
                                                       (->> (conj [:success]))))))
                    :persist    (fn [model]
                                  (->> (select-keys model #{:response})
                                       (hash-map :data)
                                       (act.hangouts/set-response response-type model-id)))})))

(defonce ^{:arglists '([response-type model-id])} response #?(:clj response* :cljs (memoize response*)))
