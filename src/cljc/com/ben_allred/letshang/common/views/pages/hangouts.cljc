(ns com.ben-allred.letshang.common.views.pages.hangouts
  (:require
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [clojure.set :as set]
    [com.ben-allred.letshang.common.resources.hangouts :as res.hangouts]
    [com.ben-allred.letshang.common.resources.hangouts.suggestions :as res.suggestions]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.store.actions.hangouts :as act.hangouts]
    [com.ben-allred.letshang.common.services.store.actions.shared :as act]
    [com.ben-allred.letshang.common.services.store.actions.users :as act.users]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.users :as users]
    [com.ben-allred.letshang.common.views.components.core :as components]
    [com.ben-allred.letshang.common.views.components.dropdown :as dropdown]
    [com.ben-allred.letshang.common.views.components.fields :as fields]
    [com.ben-allred.letshang.common.views.components.flip-move :as flip-move]
    [com.ben-allred.letshang.common.views.components.form-view :as form-view]
    [com.ben-allred.letshang.common.views.components.loading :as loading]
    [com.ben-allred.letshang.common.views.pages.hangouts.responses :as responses]
    [com.ben-allred.letshang.common.views.pages.hangouts.suggestions :as suggestions]))

(defn ^:private hangout-form [form {:keys [associates creator? invitees-only? on-saved]} & buttons]
  [form-view/form
   {:buttons  buttons
    :on-saved on-saved
    :form     form}
   (when-not invitees-only?
     [fields/input
      (-> {:label       "Name"
           :auto-focus? true}
          (res.hangouts/with-attrs form [:name]))])
   (when (seq associates)
     [dropdown/dropdown
      (-> {:label   "Invitees"
           :options (map (juxt :id users/full-name) associates)}
          (res.hangouts/with-attrs form [:invitation-ids]))])
   (when creator?
     [:<>
      [fields/checkbox
       (-> {:label "Attendees can invite people?"
            :form-field-class ["inline" "reverse"]}
           (res.hangouts/with-attrs form [:others-invite?]))]
      [fields/checkbox
       (-> {:label "Attendees can suggest when?"
            :form-field-class ["inline" "reverse"]}
           (res.hangouts/with-attrs form [:when-suggestions?]))]
      [fields/checkbox
       (-> {:label "Attendees can suggest where?"
            :form-field-class ["inline" "reverse"]}
           (res.hangouts/with-attrs form [:where-suggestions?]))]])])

(defn ^:private edit-form [hangout _change-state]
  (let [form (res.hangouts/form hangout)]
    (fn [_hangout change-state]
      [:div.layout--space-below
       [hangout-form
        form
        {:creator? true}
        [:button.button.is-info
         {:type :button :on-click #(change-state nil)}
         "Cancel"]]])))

(defn ^:private invitation-form [{:keys [hangout]}]
  (let [form (res.hangouts/form hangout)
        already-invited? (comp (conj (set (map :id (:invitations hangout))) (:created-by hangout)) :id)]
    (fn [{:keys [associates]}]
      (when-let [associates (seq (remove already-invited? associates))]
        [:div.layout--space-below
         [hangout-form
          form
          {:associates associates
           :invitees-only? true}]]))))

(defn ^:private invitation-item [{:keys [handle response] :as invitation} current-user?]
  [:li.layout--space-between.layout--align-center
   [:em handle]
   (if current-user?
     [responses/form :invitation (set/rename-keys invitation {:invitation-id :id})]
     [responses/icon response])])

(defn ^:private hangout-header [{:keys [change-state creator? state]} {:keys [hangout]}]
  (if (= state :edit)
    [edit-form hangout change-state]
    [:div.layout--space-between
     [:h1.title.is-5 {:style {:margin-bottom 0}} "Name: "]
     [:span (:name hangout)]
     (when creator?
       [:a
        {:href "#" :on-click #(change-state (when (not= state :edit) :edit))}
        [components/icon :edit]])]))

(defn ^:private hangout-who [{:keys [change-state creator? state]} resources]
  (let [{:keys [creator invitations others-invite?]} (:hangout resources)]
    [:<>
     [:h2.title.is-6 {:style {:margin-bottom 0}}
      [:a
       {:href "#" :on-click #(change-state (when (not= state :who) :who))}
       [components/icon (if (= state :who) :minus-circle :plus-circle)]
       " Who's coming?"]]
     (when (= state :who)
       [:div.layout--stack-between
        [:div.layout--inset
         [:ul.layout--stack-between
          (when-not creator?
            [invitation-item (assoc creator :response :creator) false])
          (for [invitation invitations]
            ^{:key (:id invitation)}
            [invitation-item invitation false])]]
        (when (or creator? others-invite?)
          [invitation-form resources])])]))

(defn ^:private hangout-items* [suggestion items form]
  [:div.layout--stack-between
   [:div.layout--inset
    [:ul.layout--stack-between
     [flip-move/flip-move
      {}
      (for [item items]
        ^{:key (:id item)}
        [:li.layout--space-between
         {:style {:background-color :white}}
         [suggestion item (:id @store/user)]])]]]
   form])

(defn ^:private hangout-when [{:keys [change-state creator? state]} resources]
  (let [{hangout-id :id :keys [moments when-suggestions?]} (:hangout resources)]
    [:<>
     [:h2.title.is-6 {:style {:margin-bottom 0}}
      [:a
       {:href "#" :on-click #(change-state (when (not= state :when) :when))}
       [components/icon (if (= state :when) :minus-circle :plus-circle)]
       " When?"]]
     (when (= state :when)
       [hangout-items*
        suggestions/moment-suggestion
        (sort res.suggestions/moment-sorter moments)
        (when (or creator? when-suggestions?)
          [suggestions/moment-form hangout-id])])]))

(defn ^:private hangout-where [{:keys [change-state creator? state]} resources]
  (let [{hangout-id :id :keys [locations where-suggestions?]} (:hangout resources)]
    [:<>
     [:h2.title.is-6 {:style {:margin-bottom 0}}
      [:a
       {:href "#" :on-click #(change-state (when (not= state :where) :where))}
       [components/icon (if (= state :where) :minus-circle :plus-circle)]
       " Where?"]]
     (when (= state :where)
       [hangout-items*
        suggestions/location-suggestion
        (sort res.suggestions/location-sorter locations)
        (when (or creator? where-suggestions?)
          [suggestions/location-form hangout-id])])]))

(defn ^:private hangout-view [attrs resources]
  [:div.layout--space-below.layout--stack-between
   [hangout-header attrs resources]
   [hangout-who attrs resources]
   [hangout-when attrs resources]
   [hangout-where attrs resources]])

(defn ^:private hangout* [resources]
  [fields/stateful nil [hangout-view
                        {:creator? (= (:id @store/user) (get-in resources [:hangout :created-by]))}
                        resources]])

(defn ^:private hangouts* [{:keys [hangouts]}]
  [:div
   [:div.buttons
    [:a.button.is-primary
     {:href (nav/path-for :ui/hangout-new)}
     "Create"]]
   (if (seq hangouts)
     [:ul
      (for [{:keys [creator id name]} hangouts]
        ^{:key id}
        [:li
         [:a {:href (nav/path-for :ui/hangout {:route-params {:hangout-id id}})}
          name " created by " (:handle creator)]])]
     [:div "You don't have any hangouts, yet. What are you waiting for?"])])

(defn ^:private create* [_resources]
  (let [form (res.hangouts/form)]
    (fn [{:keys [associates]}]
      [:div
       [hangout-form
        form
        {:associates associates
         :on-saved   res.hangouts/create->modify
         :creator?   true}
        (if (not (forms/ready? form))
          [:button.button.is-warning
           {:disabled true :type :button}
           "Cancel"]
          [:a.button.is-warning
           {:href (nav/path-for :ui/hangouts)}
           "Cancel"])]])))

(defn hangouts [state]
  [loading/with-status
   {:action act.hangouts/fetch-hangouts
    :keys   #{:hangouts}
    :state  state}
   hangouts*])

(defn hangout [{:keys [page] :as state}]
  [loading/with-status
   {:action (act/combine (act.hangouts/fetch-hangout (get-in page [:route-params :hangout-id]))
                         act.users/fetch-associates)
    :keys   #{:hangout :associates}
    :state  state}
   hangout*])

(defn create [state]
  [loading/with-status
   {:action act.users/fetch-associates
    :keys   #{:associates}
    :state  state}
   create*])
