(ns com.ben-allred.letshang.common.views.components.form-view
  (:require
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.dom :as dom]
    [com.ben-allred.letshang.common.views.components.loading :as loading]))

(defn form [{:keys [buttons form on-failed on-saved save-text]} & body]
  (-> [:form.form.layout--stack-between
       {:on-submit (fn [e]
                     (dom/prevent-default e)
                     (-> form
                         (forms/persist!)
                         (cond->
                           on-saved (ch/then on-saved)
                           on-failed (ch/catch on-failed))))}]
      (into body)
      (conj (cond-> [:div.buttons
                     [:button.button.is-primary
                      {:type     :submit
                       :disabled (or (not (forms/ready? form))
                                     (and (forms/attempted? form) (not (forms/valid? form))))}
                      (or save-text "Save")]]
              buttons
              (into buttons)

              (not (forms/ready? form))
              (conj [:div {:style {:margin-bottom "8px"}} [loading/spinner]])))))
