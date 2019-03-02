(ns com.ben-allred.letshang.common.views.resources.hangouts
  (:require
    #?@(:cljs [[com.ben-allred.letshang.ui.services.forms.standard :as forms.std]
               [com.ben-allred.letshang.ui.services.forms.live :as forms.live]])
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [clojure.string :as string]
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.forms.noop :as forms.noop]
    [com.ben-allred.letshang.common.services.store.actions :as actions]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.strings :as strings]
    [com.ben-allred.letshang.common.views.resources.core :as res]))

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
          (->> (actions/update-hangout (:id hangout)))
          (store/dispatch)
          (ch/then source->model)
          (ch/peek (res/toast-success "Your hangout has been saved.")
                   (res/toast-error "Something went wrong."))))))

(defn ^:private response-api [model]
  (reify
    forms/IFetch
    (fetch [_]
      (ch/resolve model))
    forms/ISave
    (save! [_ {:keys [response]}]
      (-> {:data {:response response}}
          (->> (actions/set-response (:id model)))
          (store/dispatch)
          (ch/peek (constantly nil)
                   (res/toast-error "Something went wrong."))))))

(def ^:private model->view
  {})

(def ^:private view->model
  {:name not-empty})

(def response-options
  [[:neutral "Undecided"]
   [:negative "Not attending"]
   [:positive "Attending"]])

(def response->text
  (into {:none "No response yet" :creator "Creator"} response-options))

(def response->icon
  {:none     :ban
   :positive :thumbs-up
   :negative :thumbs-down
   :neutral  :question})

(def response->level
  {:positive "is-success"
   :negative "is-warning"
   :neutral  "is-info"})

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
     :cljs (forms.live/create (response-api model) nil)))

(defn create->modify [response]
  (nav/nav-and-replace! :ui/hangout {:route-params {:hangout-id (:id response)}}))

(defn with-attrs
  ([form path]
   (with-attrs nil form path))
  ([attrs form path]
   (forms/with-attrs attrs form path model->view view->model)))
