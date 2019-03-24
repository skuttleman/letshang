(ns com.ben-allred.letshang.common.resources.hangouts.responses
  (:require
    #?(:cljs [com.ben-allred.letshang.ui.services.forms.live :as forms.live])
    [com.ben-allred.letshang.common.resources.core :as res]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.forms.noop :as forms.noop]
    [com.ben-allred.letshang.common.services.store.actions.hangouts :as act.hangouts]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn ^:private response-api [response-type model]
  (reify
    forms/IFetch
    (fetch [_]
      (ch/resolve model))
    forms/ISave
    (save! [_ {:keys [response]}]
      (-> {:data {:response response}}
          (->> (act.hangouts/set-response response-type (:id model)))
          (store/dispatch)
          (ch/peek (constantly nil)
                   (res/toast-error "Something went wrong."))))))

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

(defn form [response-type model]
  #?(:clj  (forms.noop/create nil)
     :cljs (forms.live/create (response-api response-type model) nil)))

(defn with-attrs
  ([form path]
   (with-attrs {} form path))
  ([attrs form path]
   (forms/with-attrs attrs form path nil nil)))
