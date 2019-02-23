(ns com.ben-allred.letshang.ui.services.forms.standard
  (:require
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.ui.services.forms.shared :as forms.shared]
    [com.ben-allred.letshang.common.utils.logging :as log]))


(defn create [api validator]
  (let [state (r/atom nil)
        validator (or validator (constantly nil))]
    (-> api
        (forms/fetch)
        (forms.shared/request* state validator))
    (reify
      forms/IPersist
      (attempted? [_]
        (:persist-attempted? @state))
      (persist! [this]
        (swap! state assoc :persist-attempted? true)
        (cond
          (not (forms/ready? this))
          (ch/reject {:message "Form not ready"})

          (not (forms/valid? this))
          (ch/reject {:message "Form not valid" :errors (forms/errors this)})

          (not (forms/changed? this))
          (ch/resolve @this)

          :else
          (let [model @this]
            (swap! state assoc :status :pending)
            (-> api
                (forms/save! model)
                (forms.shared/request* state validator)))))

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
      (-swap! [_ f]
        (swap! state forms.shared/swap* validator f [])
        nil)
      (-swap! [_ f a]
        (swap! state forms.shared/swap* validator f [a])
        nil)
      (-swap! [_ f a b]
        (swap! state forms.shared/swap* validator f [a b])
        nil)
      (-swap! [_ f a b xs]
        (swap! state forms.shared/swap* validator f (into [a b] xs))
        nil))))
