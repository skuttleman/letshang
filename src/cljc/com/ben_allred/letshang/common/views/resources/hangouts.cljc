(ns com.ben-allred.letshang.common.views.resources.hangouts
  (:require
    #?(:cljs [com.ben-allred.letshang.ui.services.forms.standard :as forms.std])
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [clojure.string :as string]
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.forms.noop :as forms.noop]
    [com.ben-allred.letshang.common.stubs.actions :as actions]
    [com.ben-allred.letshang.common.stubs.store :as store]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.strings :as strings]))

(defn ^:private dispatch [action]
  (action [(constantly nil)]))

(def ^:private source->model :data)

(def ^:private model->source
  (comp (partial hash-map :data)
        (f/transformer
          {:name strings/trim-to-nil})
        #(select-keys % #{:name :invitation-ids})))

(def ^:private create-api
  (reify
    forms/IFetch
    (fetch [_]
      (ch/resolve nil))
    forms/ISave
    (save! [_ model]
      (-> model
          (model->source)
          (actions/create-hangout)
          (dispatch)
          (ch/then source->model)))))

(defn ^:private edit-api [hangout]
  (reify
    forms/IFetch
    (fetch [_]
      (ch/resolve hangout))
    forms/ISave
    (save! [_ model]
      (-> model
          (model->source)
          (->> (actions/update-hangout (:id hangout)))
          (dispatch)
          (ch/then source->model)))))

(defn ^:private response-api [model]
  (reify
    forms/IFetch
    (fetch [_]
      (ch/resolve model))
    forms/ISave
    (save! [_ {:keys [response] :as next}]
      (-> {:data {:response response}}
          (->> (actions/set-response (:id model)))
          (dispatch)
          (ch/then (constantly next))))))

(def ^:private model->view
  {})

(def ^:private view->model
  {:name strings/empty-to-nil})

(defn on-modify [change-state]
  (fn [response]
    (store/dispatch [:hangout/success {:data response}])
    (change-state :normal)))

(def validator
  (f/validator {:name [(f/pred (complement string/blank?) "Your hangout must have a name")
                       (f/required "Your hangout must have a name")]
                :invitation-ids ^::f/coll-of [(f/pred uuid? "Must be a UUID")]}))

(defn form
  ([]
   (form nil))
  ([hangout]
    #?(:clj  (forms.noop/create nil)
       :cljs (forms.std/create (if hangout (edit-api hangout) create-api) validator))))

(defn response-form [model]
  #?(:clj  (forms.noop/create nil)
     :cljs (forms.std/create (response-api model) nil)))

(defn create->modify [response]
  (nav/nav-and-replace! :ui/hangout {:route-params {:hangout-id (:id response)}}))

(defn with-attrs
  ([form path]
   (with-attrs nil form path))
  ([attrs form path]
   (forms/with-attrs attrs form path model->view view->model)))
