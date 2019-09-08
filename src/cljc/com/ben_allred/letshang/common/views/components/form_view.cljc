(ns com.ben-allred.letshang.common.views.components.form-view
  (:require
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.utils.dom :as dom]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.views.components.loading :as loading]
    [com.ben-allred.vow.core :as v]))

(defn form [{:keys [buttons disabled form on-failed on-saved save-text]} & body]
  (-> [:form.form.layout--stack-between
       {:on-submit (fn [e]
                     (dom/prevent-default e)
                     (when-not disabled
                       (-> form
                           (forms/save!)
                           (cond->
                             on-saved (v/then on-saved)
                             on-failed (v/catch on-failed)))))}]
      (into body)
      (conj (cond-> [:div.buttons
                     [:button.button.is-primary
                      {:type     :submit
                       :disabled (or disabled
                                     (not (forms/ready? form))
                                     (and (forms/attempted? form) (not (forms/valid? form))))}
                      (or save-text "Save")]]
              buttons
              (into buttons)

              (not (forms/ready? form))
              (conj [:div {:style {:margin-bottom "8px"}} [loading/spinner]])))))
