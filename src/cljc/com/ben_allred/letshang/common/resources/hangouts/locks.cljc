(ns com.ben-allred.letshang.common.resources.hangouts.locks
  (:require
    #?(:cljs [com.ben-allred.letshang.ui.services.forms.live :as forms.live])
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.common.resources.core :as res]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.forms.noop :as forms.noop]
    [com.ben-allred.letshang.common.services.store.actions.hangouts :as act.hangouts]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn ^:private lock-api [lock-type id model]
  (reify
    forms/IFetch
    (fetch [_]
      (ch/resolve model))
    forms/ISave
    (save! [_ {:keys [locked?]}]
      (-> {:data {:locked? locked?}}
          (->> (act.hangouts/lock lock-type id))
          (store/dispatch)
          (ch/peek (constantly nil)
                   (res/toast-error "Something went wrong."))))))

(def lock-validator
  (f/validator
    [(f/required "Must specify a value for locked")
     (f/pred boolean? "Invalid lock value")]))

(def response->label
  {:location "Select location"
   :moment "Select moment"})

(defn form [lock-type id model]
  #?(:clj  (forms.noop/create nil)
     :cljs (forms.live/create (lock-api lock-type id model) nil)))

(defn with-attrs
  ([form path]
   (with-attrs {} form path))
  ([attrs form path]
   (forms/with-attrs attrs form path nil nil)))
