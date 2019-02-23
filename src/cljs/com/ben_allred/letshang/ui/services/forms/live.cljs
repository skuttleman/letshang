(ns com.ben-allred.letshang.ui.services.forms.live
  (:require [com.ben-allred.letshang.common.services.forms.core :as forms]
            [com.ben-allred.letshang.common.stubs.reagent :as r]
            [com.ben-allred.letshang.ui.services.forms.shared :as forms.shared]
            [com.ben-allred.letshang.common.utils.chans :as ch]))

(defn ^:private swap* [form api state validator f f-args]
  (when (forms/ready? form)
    (swap! state forms.shared/swap* validator f f-args)
    (when (and (forms/valid? form)
               (forms/changed? form))
      (swap! state assoc :status :pending)
      (-> api
          (forms/save! @form)
          (forms.shared/with-default-error)
          (forms.shared/request* state validator)))))

(defn create [api validator]
  (let [state (r/atom nil)
        validator (or validator (constantly nil))]
    (-> api
        (forms/fetch)
        (forms.shared/with-default-error)
        (forms.shared/request* state validator))
    (reify
      forms/ISync
      (ready? [_]
        (= :ready (:status @state)))

      forms/IChange
      (changed? [_]
        (forms.shared/check-for (:working @state) #(not= (:initial %) (:current %))))
      (changed? [_ path]
        (let [{:keys [current initial]} (get-in @state [:working path])]
          (not= current initial)))

      forms/ITrack
      (touch! [_ path]
        (swap! state assoc-in [:working path :touched?] true)
        nil)
      (touched? [_]
        (forms.shared/check-for (:working @state) :touched?))
      (touched? [_ path]
        (get-in @state [:working path :touched?]))

      forms/IValidate
      (errors [this]
        (let [{:keys [errors api-error]} @state]
          (cond
            api-error api-error
            (forms/ready? this) errors)))
      (valid? [this]
        (empty? (forms/errors this)))

      IDeref
      (-deref [_]
        (when-let [working-model (:working @state)]
          (forms.shared/trackable->model working-model)))

      IReset
      (-reset! [_ model]
        (reset! state (forms.shared/init validator model))
        nil)

      ISwap
      (-swap! [this f]
        (swap* this api state validator f [])
        nil)
      (-swap! [this f a]
        (swap* this api state validator f [a])
        nil)
      (-swap! [this f a b]
        (swap* this api state validator f [a b])
        nil)
      (-swap! [this f a b xs]
        (swap* this api state validator f (into [a b] xs))
        nil))))
