(ns com.ben-allred.letshang.ui.services.forms.standard
  (:require
    [com.ben-allred.letshang.common.resources.remotes.core :as remotes]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.ui.services.forms.shared :as forms.shared]
    [com.ben-allred.vow.core :as v]))

(defn create [remote validator]
  (let [state (r/atom nil)
        validator (or validator (constantly nil))]
    (-> remote
        (v/then-> (->> (forms.shared/init validator) (reset! state))))
    (reify
      forms/ISync
      (save! [this]
        (swap! state assoc :persist-attempted? true)
        (-> (cond
              (not (forms/ready? this))
              (v/reject {:message "Form not ready"})

              (not (forms/valid? this))
              (v/reject {:message "Form not valid" :errors (forms/errors this)})

              (not (forms/changed? this))
              (v/resolve @this)

              :else
              (let [model @this]
                (swap! state assoc :status :pending)
                (-> remote
                    (remotes/persist! model)
                    (forms.shared/request* state validator))))))

      forms/IBlock
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
