(ns com.ben-allred.letshang.common.resources.hangouts.conversations
  (:require
    #?(:cljs [com.ben-allred.letshang.ui.services.forms.standard :as forms.std])
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.common.resources.remotes.messages :as rem.messages]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.forms.noop :as forms.noop]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.strings :as strings]))

(def message-validator
  (f/validator {:body [(f/required "The message must have a body")
                       (f/pred strings/trim-to-nil "The message must have a body")
                       (f/pred string? "Must be a string")]}))

(def ^:private model->source
  (comp (partial hash-map :data)
        (f/transformer
          {:body strings/trim-to-nil})
        #(select-keys % #{:body})))

(defn form []
  #?(:clj  (forms.noop/create nil)
     :cljs (forms.std/create rem.messages/messages message-validator)))

(defn with-attrs
  ([form path]
   (with-attrs {} form path))
  ([attrs form path]
   (forms/with-attrs attrs form path nil nil)))
