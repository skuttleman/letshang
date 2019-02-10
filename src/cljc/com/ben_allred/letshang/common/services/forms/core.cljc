(ns com.ben-allred.letshang.common.services.forms.core
  (:require
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps])
  #?(:clj
     (:import
       (clojure.lang IAtom IDeref))))

#?(:clj (def ISwap IAtom))

;; API
(defprotocol IFetch
  (fetch [this]))

(defprotocol ISave
  (save! [this model]))

(defprotocol IDelete
  (delete! [this]))

;; FORM
(defprotocol IPersist
  (attempted? [this])
  (persist! [this]))

(defprotocol ISync
  (ready? [this])
  (status [this]))

(defprotocol IChange
  (changed? [this] [this path]))

(defprotocol ITrack
  (touch! [this path])
  (touched? [this] [this path]))

(defprotocol IValidate
  (errors [this])
  (valid? [this]))


(defn with-attrs [attrs form path model->view view->model]
  (let [attempted? (when (satisfies? IPersist form)
                     (attempted? form))
        touched? (when (satisfies? ITrack form)
                   (touched? form path))
        just-errors? (and (not (satisfies? IPersist form))
                          (not (satisfies? ITrack form)))
        errors (when (satisfies? IValidate form)
                 (get-in (errors form) path))
        to-view (get-in model->view path)
        to-model (get-in view->model path)]
    (-> attrs
        (assoc :attempted? attempted? :touched? touched?)
        (cond->
          (satisfies? IDeref form)
          (assoc :value (get-in @form path))

          (satisfies? ISwap form)
          (assoc :on-change (partial swap! form assoc-in path))

          (and (or attempted? touched? just-errors?) errors)
          (assoc :errors errors)

          to-view (update :value to-view)
          to-model (update :on-change comp to-model))
        (update :disabled #(or % (not (ready? form))))
        (update :on-blur (fn [on-blur]
                           (fn [e]
                             (touch! form path)
                             (when on-blur
                               (on-blur e))))))))
