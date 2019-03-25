(ns com.ben-allred.letshang.common.resources.hangouts
  (:require
    #?@(:cljs [[com.ben-allred.letshang.ui.services.forms.standard :as forms.std]
               [com.ben-allred.letshang.ui.services.forms.live :as forms.live]])
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [clojure.string :as string]
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.common.resources.core :as res]
    [com.ben-allred.letshang.common.resources.hangouts.suggestions :as res.suggestions]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.forms.noop :as forms.noop]
    [com.ben-allred.letshang.common.services.store.actions.hangouts :as act.hangouts]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.strings :as strings]))

(def ^:private source->model :data)

(def ^:private model->source
  (comp (partial hash-map :data)
        (f/transformer
          {:name strings/trim-to-nil})
        #(select-keys % #{:name :invitation-ids :others-invite? :when-suggestions? :where-suggestions?})))

(def ^:private create-api
  (reify
    forms/IFetch
    (fetch [_]
      (ch/resolve nil))
    forms/ISave
    (save! [_ model]
      (-> model
          (model->source)
          (act.hangouts/create-hangout)
          (store/dispatch)
          (ch/then source->model)
          (ch/peek (res/toast-success "Your hangout has been created.")
                   (res/toast-error "Something went wrong."))))))

(defn ^:private edit-api [hangout]
  (reify
    forms/IFetch
    (fetch [_]
      (ch/resolve hangout))
    forms/ISave
    (save! [_ model]
      (-> model
          (model->source)
          (->> (act.hangouts/update-hangout (:id hangout)))
          (store/dispatch)
          (ch/then source->model)
          (ch/peek (res/toast-success "Your hangout has been saved.")
                   (res/toast-error "Something went wrong."))))))

(def ^:private view->model
  {:name not-empty})

(def validator
  (f/validator [res.suggestions/who-validator
                {:name [(f/pred (every-pred string? (complement string/blank?)) "Your hangout must have a name")
                        (f/required "Your hangout must have a name")]}]))

(defn form
  ([]
   (form nil))
  ([hangout]
    #?(:clj  (forms.noop/create nil)
       :cljs (forms.std/create (if hangout (edit-api hangout) create-api) validator))))

(defn create->modify [response]
  (nav/nav-and-replace! :ui/hangout {:route-params {:hangout-id (:id response)}}))

(defn with-attrs
  ([form path]
   (with-attrs nil form path))
  ([attrs form path]
   (forms/with-attrs attrs form path nil view->model)))
