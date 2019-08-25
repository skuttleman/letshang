(ns com.ben-allred.letshang.common.resources.hangouts.suggestions
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
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(def windows [:any-time :morning :mid-day :afternoon :after-work :evening :night :twilight])

(def who-validator
  (f/validator {:invitation-ids ^::f/coll-of [[(f/required "Must not be missing")
                                               (f/pred uuid? "Must be a UUID")]]}))

(def when-validator
  (f/validator
    {:date   [(f/required "You must select a date")
              (f/pred #(not (dates/before? % (dates/today))) "Cannot be in the past")]
     :window [(f/required "You must select a window")
              (f/pred (set windows) "Not a valid window")]}))

(def where-validator
  (f/validator
    {:name [(f/required "You must select a place")
            (f/pred string? "Must be a string")]}))

(def ^:private model->source
  (partial hash-map :data))

(defn ^:private suggest-api [initial action-fn]
  (let [ready? (r/atom true)]
    (reify
      forms/IResource
      (fetch [_]
        (ch/resolve initial))
      (persist! [_ model]
        (reset! ready? false)
        (-> model
            (model->source)
            (action-fn)
            (store/dispatch)
            (ch/peek (fn [_] (reset! ready? true)))
            (ch/peek (constantly nil)
                     (res/toast-error "Something went wrong."))
            (ch/then (constantly initial))))

      forms/IBlock
      (ready? [_]
        @ready?))))

(defn ^:private sorter* [k]
  (fn [{response-counts-1 :response-counts value-1 k} {response-counts-2 :response-counts value-2 k}]
    (let [score (compare (- (:positive response-counts-2 0) (:negative response-counts-2 0))
                         (- (:positive response-counts-1 0) (:negative response-counts-1 0)))]
      (if (zero? score)
        (let [score-2 (compare (reduce + 0 (vals response-counts-2)) (reduce + 0 (vals response-counts-1)))]
          (if (zero? score-2)
            (compare value-1 value-2)
            score-2))
        score))))

(def moment-sorter (sorter* :date))

(def location-sorter (sorter* :name))

(def ^:private default-when
  {:window :any-time})

(defn who-form [hangout-id]
  #?(:cljs (forms.std/create (suggest-api nil (partial act.hangouts/suggest :who hangout-id)) who-validator)
     :default (forms.noop/create nil)))

(defn when-form [hangout-id]
  #?(:cljs (forms.std/create (suggest-api default-when (partial act.hangouts/suggest :when hangout-id)) when-validator)
     :default (forms.noop/create nil)))

(defn where-form [hangout-id]
  #?(:cljs (forms.std/create (suggest-api nil (partial act.hangouts/suggest :where hangout-id)) where-validator)
     :default (forms.noop/create nil)))

(defn with-attrs
  ([form path]
   (with-attrs nil form path))
  ([attrs form path]
   (forms/with-attrs attrs form path nil nil)))
