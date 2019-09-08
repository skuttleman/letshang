(ns com.ben-allred.letshang.common.resources.sign-up
  (:require
    #?(:cljs [com.ben-allred.letshang.ui.services.forms.standard :as forms.std])
    [clojure.string :as string]
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.common.resources.remotes.users :as rem.users]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.forms.noop :as forms.noop]
    [com.ben-allred.letshang.common.services.validators :as validators]))

(def ^:private model->source
  (partial hash-map :data))

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
  #?(:cljs    (forms.std/create (rem.users/sign-up new-user) validator)
     :default (forms.noop/create new-user)))
