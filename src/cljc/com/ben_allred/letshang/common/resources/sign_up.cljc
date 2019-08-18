(ns com.ben-allred.letshang.common.resources.sign-up
  (:require
    #?(:cljs [com.ben-allred.letshang.ui.services.forms.standard :as forms.std])
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [clojure.string :as string]
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.common.resources.core :as res]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.forms.noop :as forms.noop]
    [com.ben-allred.letshang.common.services.store.actions.users :as act.users]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.services.validators :as validators]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.chans :as ch]))

(def ^:private model->source
  (partial hash-map :data))

(defn ^:private api []
  (let [ready? (r/atom true)]
    (reify
      forms/ISync
      (save! [_ {:keys [model]}]
        (reset! ready? false)
        (-> model
            (model->source)
            (act.users/register-user)
            (store/dispatch)
            (ch/peek (fn [_] (reset! ready? true)))
            (ch/peek (fn [_]
                       (nav/go-to! (nav/path-for :auth/login {:query-params (select-keys model #{:email})})))
                     (res/toast-error "Something went wrong."))))
      (ready? [_]
        @ready?))))

(def validator
  (f/validator
    {:email         [(f/required "Email is required")
                     (f/pred validators/email? "Not a valid email")]
     :handle        [(f/required "Handle is required")
                     (f/identity string/lower-case "Handle must be lower-case")]
     :first-name    [(f/required "First name is required")
                     (f/pred string? "Must be a string")]
     :last-name     [(f/required "Last name is required")
                     (f/pred string? "Must be a string")]
     :mobile-number [(f/required "Mobile number is required")
                     (f/pred validators/phone-number? "Not a valid phone number")]}))

(def ^:private view->model
  {:email         (comp not-empty #(when % (string/lower-case %)))
   :handle        (comp not-empty #(when % (string/lower-case %)))
   :first-name    not-empty
   :last-name     not-empty
   :mobile-number not-empty})

(defn with-attrs [attrs form path]
  (forms/with-attrs attrs form path nil view->model))

(defn form [new-user]
  #?(:cljs    (forms.std/create new-user (api) validator)
     :default (forms.noop/create new-user)))
