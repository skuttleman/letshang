(ns com.ben-allred.letshang.common.resources.hangouts.conversations
  (:require
    #?(:cljs [com.ben-allred.letshang.ui.services.forms.standard :as forms.std])
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.common.resources.core :as res]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.forms.noop :as forms.noop]
    [com.ben-allred.letshang.common.services.store.actions.hangouts :as act.hangouts]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.strings :as strings]))

(def message-validator
  (f/validator {:body [(f/required "The message must have a body")
                       (f/pred strings/trim-to-nil "The message must have a body")
                       (f/pred string? "Must be a string")]}))

(def ^:private model->source
  (comp (partial hash-map :data)
        (f/transformer
          {:body strings/trim-to-nil})
        #(select-keys % #{:body})))

(defn ^:private response-api [hangout-id]
  (let [ready? (r/atom true)]
    (reify
      forms/ISync
      (save! [_ {:keys [model]}]
        (reset! ready? false)
        (-> model
            (model->source)
            (->> (act.hangouts/save-message hangout-id))
            (store/dispatch)
            (ch/peek (fn [_] (reset! ready? true)))
            (ch/peek (constantly nil)
                     (res/toast-error "Something went wrong."))
            (ch/then (constantly nil))))
      (ready? [_]
        @ready?))))

(defn form [hangout-id]
  #?(:clj  (forms.noop/create nil)
     :cljs (forms.std/create nil (response-api hangout-id) message-validator)))

(defn with-attrs
  ([form path]
   (with-attrs {} form path))
  ([attrs form path]
   (forms/with-attrs attrs form path nil nil)))
