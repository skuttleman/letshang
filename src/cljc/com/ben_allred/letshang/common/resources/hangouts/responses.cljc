(ns com.ben-allred.letshang.common.resources.hangouts.responses
  (:require
    #?(:cljs [com.ben-allred.letshang.ui.services.forms.live :as forms.live])
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.common.resources.core :as res]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.forms.noop :as forms.noop]
    [com.ben-allred.letshang.common.services.store.actions.hangouts :as act.hangouts]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.fns #?(:clj :refer :cljs :refer-macros) [=> =>>]]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn ^:private response-api [response-type user-id model-id]
  (let [ready? (r/atom false)]
    (reify
      forms/IResource
      (fetch [_]
        (-> (store/reaction [:moments])
            (ch/from-reaction)
            (ch/then (=>> (filter (comp #{model-id} :id))
                          (first)
                          (:responses)
                          (filter (comp #{user-id} :user-id))
                          (first)))
            (ch/then (=> (select-keys #{:user-id :response})
                         (assoc :id model-id)))
            (ch/peek (fn [_] (reset! ready? true)))))
      (persist! [_ model]
        (reset! ready? false)
        (-> {:data (select-keys model #{:response})}
            (->> (act.hangouts/set-response response-type model-id))
            (store/dispatch)
            (ch/peek (fn [_] (reset! ready? true)))
            (ch/peek (constantly nil)
                     (res/toast-error "Something went wrong."))))

      forms/IBlock
      (ready? [_]
        @ready?))))

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

(defn form [response-type user-id model-id]
  #?(:clj  (forms.noop/create nil)
     :cljs (forms.live/create (response-api response-type user-id model-id) (constantly nil))))

(defn with-attrs
  ([form path]
   (with-attrs {} form path))
  ([attrs form path]
   (forms/with-attrs attrs form path nil nil)))
