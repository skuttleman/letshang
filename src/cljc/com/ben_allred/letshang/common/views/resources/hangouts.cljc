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
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.strings :as strings]))

(defn ^:private dispatch [action]
  (action [(constantly nil)]))

(def ^:private source->model :data)

(def ^:private model->source
  (comp (partial hash-map :data)
        (f/transformer
          {:name strings/trim-to-nil})))

(def ^:private create-api
  (reify
    forms/IFetch
    (fetch [_]
      (ch/resolve (source->model nil)))
    forms/ISave
    (save! [_ model]
      (-> model
          (model->source)
          (actions/create-hangout)
          (dispatch)
          (ch/then source->model)))))

(defn ^:private edit-api [hangout-id]
  (reify
    forms/IFetch
    (fetch [_]
      (-> (actions/fetch-hangout hangout-id)
          (dispatch)
          (ch/then source->model)))
    forms/ISave
    (save! [_ model]
      (-> model
          (model->source)
          (->> (actions/update-hangout hangout-id))
          (dispatch)
          (ch/then source->model)))))

(def ^:private model->view
  {})

(def ^:private view->model
  {:name strings/empty-to-nil})

(def validator
  (f/validator
    {:name [(f/pred (complement string/blank?) "Your hangout must have a name")
            (f/required "Your hangout must have a name")]}))

(defn form
  ([]
   (form nil))
  ([hangout-id]
    #?(:clj  (forms.noop/create nil)
       :cljs (forms.std/create (if hangout-id (edit-api hangout-id) create-api) validator))))

(defn create->modify [response]
  (nav/nav-and-replace! :ui/hangout {:route-params {:hangout-id (:id response)}}))

(defn with-attrs
  ([form path]
   (with-attrs nil form path))
  ([attrs form path]
   (forms/with-attrs attrs form path model->view view->model)))
