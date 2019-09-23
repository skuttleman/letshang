(ns com.ben-allred.letshang.common.resources.remotes.messages
  (:require
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.common.resources.core :as res]
    [com.ben-allred.letshang.common.resources.remotes.impl :as r.impl]
    [com.ben-allred.letshang.common.services.store.actions.hangouts :as act.hangouts]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.strings :as strings]
    [com.ben-allred.vow.core :as v])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(def ^:private model->source
  (comp (partial hash-map :data)
        (f/transformer
          {:body strings/trim-to-nil})
        #(select-keys % #{:body})))

(def reaction
  (reify IDeref
    #?(:clj  (deref [_] [:success])
       :cljs (-deref [_] [:success]))))

(defonce messages
  (let [hangout-id (store/reaction [:page :route-params :hangout-id])]
    (r.impl/create {:reaction reaction
                    :persist  (fn [model]
                                (act.hangouts/save-message @hangout-id (model->source model)))})))
