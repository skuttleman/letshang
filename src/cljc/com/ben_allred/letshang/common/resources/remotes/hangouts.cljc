(ns com.ben-allred.letshang.common.resources.remotes.hangouts
  (:require
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.common.resources.remotes.impl :as r.impl]
    [com.ben-allred.letshang.common.services.store.actions.hangouts :as act.hangouts]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.fns #?(:clj :refer :cljs :refer-macros) [=>]]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.strings :as strings]
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]))

(def model->source
  (comp (partial hash-map :data)
        (f/transformer
          {:name strings/trim-to-nil})
        #(select-keys % #{:name :invitation-ids :others-invite? :when-suggestions? :where-suggestions?})))

(defn ^:private deref* [match? action [status value]]
  (when (or (= :init status) (not match?))
    (store/dispatch action))
  (when match?
    value))

(defonce hangouts
  (r.impl/create {:fetch       (constantly act.hangouts/fetch-hangouts)
                  :reaction    (store/reaction [:hangouts])
                  :invalidate! (constantly [:hangouts/invalidate!])}))

(defonce hangout
  (let [hangout-id (store/reaction [:page :route-params :hangout-id])
        hangout (store/reaction [:hangout])
        reaction (r/make-reaction #(if @hangout-id
                                     @hangout
                                     [:success {:where-suggestions? true :when-suggestions? true :others-invite? false}]))]
    (r.impl/create {:match?      #(= @hangout-id (:id (second @reaction)))
                    :fetch       #(if-some [id @hangout-id]
                                    (act.hangouts/fetch-hangout id)
                                    [::store/noop])
                    :reaction    reaction
                    :persist     (fn [model]
                                   (if-some [id @hangout-id]
                                     (act.hangouts/update-hangout id (model->source model))
                                     (act.hangouts/create-hangout (model->source model))))
                    :invalidate! #(if-some [id @hangout-id]
                                    [:hangout/invalidate! id]
                                    [::store/noop])})))
