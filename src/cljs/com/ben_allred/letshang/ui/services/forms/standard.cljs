(ns com.ben-allred.letshang.ui.services.forms.standard
  (:require
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.ui.services.forms.shared :as forms.shared]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn create [model sync validator]
  (let [state (r/atom (forms.shared/init validator model))
        validator (or validator (constantly nil))]
    (reify
      forms/ISync
      (save! [this]
        (swap! state assoc :persist-attempted? true)
        (-> (cond
              (not (forms/ready? this))
              (ch/reject {:message "Form not ready"})

              (not (forms/valid? this))
              (ch/reject {:message "Form not valid" :errors (forms/errors this)})

              (not (forms/changed? this))
              (ch/resolve @this)

              :else
              (let [model @this]
                (swap! state assoc :status :pending)
                (-> sync
                    (forms/save! {:model model})
                    (forms.shared/request* state validator))))))
      (ready? [_]
        (= :ready (:status @state)))

      forms/IChange
      (changed? [_]
        (forms.shared/check-for (:working @state) #(not= (:initial %) (:current %))))
      (changed? [_ path]
        (let [{:keys [current initial]} (get-in @state [:working path])]
          (not= current initial)))

      forms/ITrack
      (attempted? [_]
        (:persist-attempted? @state))
      (visit! [_ path]
        (swap! state assoc-in [:working path :visited?] true)
        nil)
      (visited? [_ path]
        (let [{:keys [working persist-attempted?]} @state]
          (or persist-attempted?
              (get-in working [path :visited?]))))

      forms/IValidate
      (errors [this]
        (when (forms/ready? this)
          (let [{:keys [errors api-errors model]} @state]
            (->> (for [[path m] api-errors
                       [value errors'] m
                       :when (= value (get-in model path))]
                   [path errors'])
                 (reduce (fn [m [path e]] (update-in m path concat e)) errors)))))

      IDeref
      (-deref [_]
        (:model @state))

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
