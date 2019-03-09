(ns com.ben-allred.letshang.common.views.pages.hangouts.suggestions
  (:require
    #?(:cljs [com.ben-allred.letshang.ui.services.forms.standard :as forms.std])
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.views.components.calendar :as calendar]
    [com.ben-allred.letshang.common.views.resources.hangouts.suggestions :as res.suggestions]
    [com.ben-allred.letshang.common.views.components.form-view :as form-view]
    [com.ben-allred.letshang.common.views.components.core :as components]
    [com.ben-allred.letshang.common.views.components.dropdown :as dropdown]
    [com.ben-allred.letshang.common.utils.strings :as strings]))

(defn window-button [attrs]
  [:button.button
   (-> attrs
       (select-keys #{:class :disabled :on-blur :on-click :ref})
       (assoc :type :button))
   (get (:options-by-id attrs) (first (:value attrs)) "Pick a time…")
   [:span
    {:style {:margin-left "10px"}}
    [components/icon (if (:open? attrs) :chevron-up :chevron-down)]]])

(defn moment [hangout-id]
  (let [form (res.suggestions/form hangout-id)]
    (fn [_hangout-id]
      [form-view/form
       {:inline?   true
        :form      form
        :save-text "Suggest"}
       [calendar/picker
        (-> {:button-text "Pick a day…"}
            (res.suggestions/with-attrs form [:date]))]
       [dropdown/dropdown
        (-> {:options (map (juxt identity (comp #(strings/titlize % " ") name)) res.suggestions/windows)
             :button-control window-button}
            (res.suggestions/with-attrs form [:window])
            (dropdown/oneable))]])))
