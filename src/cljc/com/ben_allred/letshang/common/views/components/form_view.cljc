(ns com.ben-allred.letshang.common.views.components.form-view
  (:require
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.dom :as dom]
    [com.ben-allred.letshang.common.views.components.loading :as loading]))

(defn form [{:keys [save-text buttons inline? on-saved on-failed form]} & body]
  (-> [:form.form
       {:on-submit (fn [e]
                     (dom/prevent-default e)
                     (-> form
                         (forms/persist!)
                         (cond->
                           on-saved (ch/then on-saved)
                           on-failed (ch/catch on-failed))))
        :class [(if inline? "layout--space-between" "layout--stack-between")]
        :style (when inline? {:align-items :flex-start})}]
      (into body)
      (conj (-> [:div.buttons
                 [:button.button.is-primary
                  {:type     :submit
                   :disabled (or (not (forms/ready? form))
                                 (and (forms/attempted? form) (not (forms/valid? form))))}
                  (or save-text "Save")]]
                (into buttons)
                (cond->
                  (not (forms/ready? form))
                  (conj [:div {:style {:margin-bottom "8px"}} [loading/spinner]]))))))
