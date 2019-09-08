(ns com.ben-allred.letshang.common.resources.hangouts.responses
  (:require
    #?(:cljs [com.ben-allred.letshang.ui.services.forms.live :as forms.live])
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.common.resources.remotes.responses :as rem.responses]
    [com.ben-allred.letshang.common.resources.core :as res]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.forms.noop :as forms.noop]
    [com.ben-allred.letshang.common.services.store.actions.hangouts :as act.hangouts]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.fns #?(:clj :refer :cljs :refer-macros) [=> =>>]]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.proms :as proms]
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]))

(def response-validator
  (f/validator
    [(f/required "Must specify a response")
     (f/pred #{:positive :negative :neutral} "Invalid response value")]))

(def response->label
  {:invitation "Are you coming?"
   :moment     "Are you available?"
   :location   "How does this place sound?"})

(def response->options
  {:invitation [[:neutral "Not sure"]
                [:negative "I'm out"]
                [:positive "I'm in"]]
   :moment     [[:neutral "Maybe"]
                [:negative "No can do"]
                [:positive "Works for me"]]
   :location   [[:neutral "We'll see"]
                [:negative "Bad idea"]
                [:positive "Good idea"]]})

(def response->text
  (into {:none "No response yet" :creator "Creator"} (response->options :invitation)))

(def response->icon
  {:none     :ban
   :positive :thumbs-up
   :negative :thumbs-down
   :neutral  :question})

(def response->level
  {:positive "is-success"
   :negative "is-warning"
   :neutral  "is-info"})

(defn sub [response-type form]
  (let [response-fn (keywords/join :- [response-type :id])]
    (fn [[_ {:keys [data]}]]
      (let [model @form]
        (some->> data
                 (:responses)
                 (colls/find (comp #{[(:user-id model) (:id model)]} (juxt :user-id response-fn)))
                 (:response)
                 (swap! form assoc :response))))))

(defn form [response-type model-id]
  #?(:clj  (forms.noop/create @(rem.responses/response response-type model-id))
     :cljs (forms.live/create (rem.responses/response response-type model-id)
                              (constantly nil))))

(defn with-attrs
  ([form path]
   (with-attrs {} form path))
  ([attrs form path]
   (forms/with-attrs attrs form path nil nil)))
