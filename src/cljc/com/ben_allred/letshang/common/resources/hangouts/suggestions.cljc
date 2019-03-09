(ns com.ben-allred.letshang.common.resources.hangouts.suggestions
  (:require
    #?(:cljs [com.ben-allred.letshang.ui.services.forms.standard :as forms.std])
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.common.resources.core :as res]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.forms.noop :as forms.noop]
    [com.ben-allred.letshang.common.services.store.actions :as actions]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(def when-validator
  (f/validator
    {:date   [(f/required "You must select a date")
              (f/pred #(not (dates/before? % (dates/today))) "Cannot be in the past")]
     :window (f/required "You must select a window")}))

(def where-validator
  (f/validator
    {:name (f/required "You must select a place")}))

(def ^:private model->source
  (comp (partial hash-map :data)
        (f/transformer
          {:date dates/->inst})))

(defn ^:private suggest-api [initial action-fn]
  (reify
    forms/IFetch
    (fetch [_]
      (ch/resolve initial))
    forms/ISave
    (save! [_ model]
      (-> model
          (model->source)
          (action-fn)
          (store/dispatch)
          (ch/peek (constantly nil)
                   (res/toast-error "Something went wrong."))
          (ch/then (constantly initial))))))

(def windows [:any-time :morning :mid-day :afternoon :after-work :evening :night :twilight])

(defn moment-sorter [{response-counts-1 :response-counts date-1 :date} {response-counts-2 :response-counts date-2 :date}]
  (let [score (compare (- (:positive response-counts-2 0) (:negative response-counts-2 0))
                       (- (:positive response-counts-1 0) (:negative response-counts-1 0)))]
    (if (zero? score)
      (let [score-2 (compare (reduce + 0 (vals response-counts-2)) (reduce + 0 (vals response-counts-1)))]
        (if (zero? score-2)
          (compare date-1 date-2)
          score-2))
      score)))

(defn location-sorter [{response-counts-1 :response-counts name-1 :name} {response-counts-2 :response-counts name-2 :name}]
  (let [score (compare (- (:positive response-counts-2 0) (:negative response-counts-2 0))
                       (- (:positive response-counts-1 0) (:negative response-counts-1 0)))]
    (if (zero? score)
      (let [score-2 (compare (reduce + 0 (vals response-counts-2)) (reduce + 0 (vals response-counts-1)))]
        (if (zero? score-2)
          (compare name-1 name-2)
          score-2))
      score)))

(defn when-form [hangout-id]
  #?(:cljs (forms.std/create (suggest-api {:window :any-time} (partial actions/suggest-when hangout-id)) when-validator)
     :default (forms.noop/create nil)))

(defn where-form [hangout-id]
  #?(:cljs (forms.std/create (suggest-api nil (partial actions/suggest-where hangout-id)) where-validator)
     :default (forms.noop/create nil)))

(defn with-attrs
  ([form path]
   (with-attrs nil form path))
  ([attrs form path]
   (forms/with-attrs attrs form path nil nil)))
