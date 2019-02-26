(ns com.ben-allred.letshang.common.services.forms.core
  (:require
    [com.ben-allred.letshang.common.utils.logging :as log])
  #?(:clj
     (:import
       (clojure.lang IAtom IDeref))))

(defn ^:private derefable? [value]
  #?(:clj  (.isInstance IDeref value)
     :cljs (satisfies? IDeref value)))

(defn ^:private swapable? [value]
  #?(:clj  (.isInstance IAtom value)
     :cljs (satisfies? ISwap value)))

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
  (visit! [this path])
  (visited? [this path]))

(defprotocol IValidate
  (errors [this])
  (valid? [this]))


(defn with-attrs [attrs form path model->view view->model]
  (let [attempted? (when (satisfies? IPersist form)
                     (attempted? form))
        visited? (when (satisfies? ITrack form)
                   (visited? form path))
        just-errors? (and (not (satisfies? IPersist form))
                          (not (satisfies? ITrack form)))
        errors (when (satisfies? IValidate form)
                 (get-in (errors form) path))
        to-view (get-in model->view path)
        to-model (get-in view->model path)]
    (-> attrs
        (assoc :attempted? attempted? :visited? visited?)
        (cond->
          (derefable? form)
          (assoc :value (get-in @form path))

          (swapable? form)
          (assoc :on-change (partial swap! form assoc-in path))

          (and (or attempted? visited? just-errors?) errors)
          (assoc :errors errors)

          to-view (update :value to-view)
          to-model (update :on-change comp to-model))
        (update :disabled #(or % (not (ready? form))))
        (update :on-blur (fn [on-blur]
                           (fn [e]
                             (visit! form path)
                             (when on-blur
                               (on-blur e))))))))
