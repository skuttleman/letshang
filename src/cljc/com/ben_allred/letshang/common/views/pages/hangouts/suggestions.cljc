(ns com.ben-allred.letshang.common.views.pages.hangouts.suggestions
  (:require
    #?(:cljs [com.ben-allred.letshang.ui.services.forms.standard :as forms.std])
    [com.ben-allred.letshang.common.resources.hangouts.suggestions :as res.suggestions]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.strings :as strings]
    [com.ben-allred.letshang.common.utils.users :as users]
    [com.ben-allred.letshang.common.views.components.calendar :as calendar]
    [com.ben-allred.letshang.common.views.components.core :as components]
    [com.ben-allred.letshang.common.views.components.dropdown :as dropdown]
    [com.ben-allred.letshang.common.views.components.fields :as fields]
    [com.ben-allred.letshang.common.views.components.form-view :as form-view]
    [com.ben-allred.letshang.common.views.pages.hangouts.responses :as responses]))

(defn ^:private suggestion* [header responses form]
  [:<>
   [:div
    {:style {:width "100%"}}
    header]
   responses
   form])

(defn ^:private suggestion-responses [{:keys [positive negative neutral]}]
  [:div.layout--space-between
   (when positive [responses/icon :positive positive])
   (when negative [responses/icon :negative negative])
   (when neutral [responses/icon :neutral neutral])])

(defn ^:private response-form [response-type id user-id responses]
  [responses/form response-type (->> responses
                                     (colls/find (comp #{user-id} :user-id))
                                     (:response)
                                     (assoc {:id id :user-id user-id} :response))])

(defn window-button [attrs]
  [:button.button
   (-> attrs
       (select-keys #{:class :disabled :on-blur :on-click :ref})
       (assoc :type :button))
   (get (:options-by-id attrs) (first (:value attrs)) "Pick a time…")
   [:span
    {:style {:margin-left "10px"}}
    [components/icon (if (:open? attrs) :chevron-up :chevron-down)]]])

(defn invitation-form [hangout _associates]
  (let [form (res.suggestions/who-form (:id hangout))]
    (fn [hangout associates]
      (let [already-invited? (comp (conj (set (map :id (:invitations hangout))) (:created-by hangout)) :id)]
        (when-let [associates (seq (remove already-invited? associates))]
          [:div.layout--space-below
           [form-view/form
            {:inline?   true
             :form      form
             :save-text "Invite"}
            [dropdown/dropdown
             (-> {:label   "Invitees"
                  :options (map (juxt :id users/full-name) associates)}
                 (res.suggestions/with-attrs form [:invitation-ids]))]]])))))

(defn moment-form [hangout-id]
  (let [form (res.suggestions/when-form hangout-id)]
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

(defn location-form [hangout-id]
  (let [form (res.suggestions/where-form hangout-id)]
    (fn [_hangout-id]
      [form-view/form
       {:inline?   true
        :form      form
        :save-text "Suggest"}
       [fields/input
        (-> {:label "Name of the place"}
            (res.suggestions/with-attrs form [:name]))]])))

(defn location-suggestion [{location-id :id :keys [name response-counts responses]} user-id]
  [suggestion*
   name
   [suggestion-responses response-counts]
   [response-form :location location-id user-id responses]])

(defn moment-suggestion [{moment-id :id :keys [date response-counts responses window]} user-id]
  [suggestion*
   [components/tooltip
    {:text (str (dates/format date :date/view) ": " (strings/titlize (keywords/safe-name window) " "))}
    (dates/relative date)]
   [suggestion-responses response-counts]
   [response-form :moment moment-id user-id responses]])
