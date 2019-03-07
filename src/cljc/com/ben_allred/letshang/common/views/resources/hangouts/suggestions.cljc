(ns com.ben-allred.letshang.common.views.resources.hangouts.suggestions
  (:require
    #?(:cljs [com.ben-allred.letshang.ui.services.forms.standard :as forms.std])
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.forms.noop :as forms.noop]
    [com.ben-allred.letshang.common.services.store.actions :as actions]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.views.resources.core :as res]))

(def validator
  (f/validator
    {:date   [(f/required "You must select a date")
              (f/pred #(not (dates/before? % (dates/today))) "Cannot be in the past")]
     :window (f/required "You must select a window")}))

(def ^:private model->source
  (comp (partial hash-map :data)
        (f/transformer
          {:date dates/->inst})))

(defn ^:private suggest-api [hangout-id]
  (reify
    forms/IFetch
    (fetch [_]
      (ch/resolve {:window :any-time}))
    forms/ISave
    (save! [_ model]
      (-> model
          (model->source)
          (->> (actions/suggest-when hangout-id))
          (store/dispatch)
          (ch/peek (constantly nil)
                   (res/toast-error "Something went wrong."))
          (ch/then (constantly {:window :any-time}))))))

(def windows [:any-time :morning :mid-day :afternoon :after-work :evening :night :twilight])

(defn moment-sorter [response-counts-1 response-counts-2]
  (let [score (compare (- (:positive response-counts-2 0) (:negative response-counts-2 0))
                       (- (:positive response-counts-1 0) (:negative response-counts-1 0)))]
    (if (zero? score)
      (compare (reduce + 0 (vals response-counts-2)) (reduce + 0 (vals response-counts-1)))
      score)))

(defn form [hangout-id]
  #?(:cljs (forms.std/create (suggest-api hangout-id) validator)
     :default (forms.noop/create nil)))

(defn with-attrs
  ([form path]
   (with-attrs nil form path))
  ([attrs form path]
   (forms/with-attrs attrs form path nil nil)))
