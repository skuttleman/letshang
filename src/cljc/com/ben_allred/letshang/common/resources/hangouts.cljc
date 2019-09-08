(ns com.ben-allred.letshang.common.resources.hangouts
  (:require
    #?@(:cljs [[com.ben-allred.letshang.ui.services.forms.standard :as forms.std]
               [com.ben-allred.letshang.ui.services.forms.live :as forms.live]])
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [clojure.string :as string]
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.common.resources.hangouts.suggestions :as res.suggestions]
    [com.ben-allred.letshang.common.resources.remotes.hangouts :as rem.hangouts]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.forms.noop :as forms.noop]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.vow.core :as v]))

(def ^:private view->model
  {:name not-empty})

(def validator
  (f/validator [res.suggestions/who-validator
                {:name [(f/pred (every-pred string? (complement string/blank?)) "Your hangout must have a name")
                        (f/required "Your hangout must have a name")]}]))

(defn form []
  #?(:clj  (forms.noop/create nil)
     :cljs (forms.std/create rem.hangouts/hangout validator)))

(defn create->modify [response]
  (nav/nav-and-replace! :ui/hangout {:route-params {:hangout-id (get-in response [:data :id])
                                                    :section    :invitations}}))

(defn with-attrs
  ([form path]
   (with-attrs nil form path))
  ([attrs form path]
   (forms/with-attrs attrs form path nil view->model)))
