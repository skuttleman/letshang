(ns com.ben-allred.letshang.common.views.pages.hangouts
  (:require
    #?(:cljs [com.ben-allred.letshang.ui.services.ws :as ws])
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [clojure.set :as set]
    [com.ben-allred.letshang.common.resources.hangouts :as res.hangouts]
    [com.ben-allred.letshang.common.resources.hangouts.suggestions :as res.suggestions]
    [com.ben-allred.letshang.common.resources.remotes.hangouts :as rem.hangouts]
    [com.ben-allred.letshang.common.resources.remotes.invitations :as rem.invitations]
    [com.ben-allred.letshang.common.resources.remotes.locations :as rem.locations]
    [com.ben-allred.letshang.common.resources.remotes.moments :as rem.moments]
    [com.ben-allred.letshang.common.resources.remotes.users :as rem.users]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.users :as users]
    [com.ben-allred.letshang.common.views.components.core :as components]
    [com.ben-allred.letshang.common.views.components.dropdown :as dropdown]
    [com.ben-allred.letshang.common.views.components.fields :as fields]
    [com.ben-allred.letshang.common.views.components.flip-move :as flip-move]
    [com.ben-allred.letshang.common.views.components.form-view :as form-view]
    [com.ben-allred.letshang.common.views.components.loading :as loading]
    [com.ben-allred.letshang.common.views.pages.hangouts.conversations :as conversations]
    [com.ben-allred.letshang.common.views.pages.hangouts.responses :as responses]
    [com.ben-allred.letshang.common.views.pages.hangouts.suggestions :as suggestions]))

(defn ^:private hangout-form [form {:keys [associates creator? on-saved]} & buttons]
  [form-view/form
   {:buttons  buttons
    :on-saved on-saved
    :form     form}
   [fields/input
    (-> {:label       "Name"
         :auto-focus? true}
        (res.hangouts/with-attrs form [:name]))]
   (when (seq associates)
     [dropdown/dropdown
      (-> {:label   "Invitees"
           :options (map (juxt :id users/full-name) associates)}
          (res.hangouts/with-attrs form [:invitation-ids]))])
   (when creator?
     [:<>
      [fields/checkbox
       (-> {:label            "Attendees can invite people?"
            :form-field-class ["inline" "reverse"]}
           (res.hangouts/with-attrs form [:others-invite?]))]
      [fields/checkbox
       (-> {:label            "Attendees can suggest when?"
            :form-field-class ["inline" "reverse"]}
           (res.hangouts/with-attrs form [:when-suggestions?]))]
      [fields/checkbox
       (-> {:label            "Attendees can suggest where?"
            :form-field-class ["inline" "reverse"]}
           (res.hangouts/with-attrs form [:where-suggestions?]))]])])

(defn ^:private edit-form [_change-state]
  (let [form (res.hangouts/form)]
    (fn [change-state]
      [:div.layout--space-below
       [hangout-form
        form
        {:creator? true}
        [:button.button.is-info
         {:type :button :on-click #(change-state nil)}
         "Cancel"]]])))

(defn ^:private invitation-item [{:keys [handle response] :as invitation} current-user?]
  [:li.layout--space-between.layout--align-center
   [:em handle]
   (if current-user?
     [responses/form :invitation (set/rename-keys invitation {:invitation-id :id})]
     [responses/icon response])])

(defn ^:private hangout-header [{:keys [change-state creator? state]} {:keys [hangout page]}]
  (let [current-section (get-in page [:route-params :section])]
    [:div.layout--stack-between
     (if (= state :edit)
       [edit-form change-state]
       [:div.layout--space-between
        [:h1.title.is-5 {:style {:margin-bottom 0}} "Name: "]
        [:span (:name hangout)]
        (when creator?
          [:a
           {:href "#" :on-click #(change-state (when (not= state :edit) :edit))}
           [components/icon :edit]])])
     [:div.tabs.hangout-tabs
      [:ul
       (for [[label section] [["Who" :invitations] ["What" :conversation] ["When" :moments] ["Where" :locations]]]
         ^{:key section}
         [:li
          {:class [(when (= current-section section) "is-active")]}
          [:a {:href     (nav/path-for :ui/hangout {:route-params {:hangout-id (:id hangout) :section section}})
               :on-click #(change-state nil)}
           label]])]]]))

(defn ^:private hangout-items* [suggestion items]
  (let [user-id (:id @store/user)]
    [:div.layout--inset
     [:ul.layout--stack-between
      [flip-move/flip-move
       {:enter-animation :none
        :leave-animation :none}
       (for [item items]
         ^{:key (:id item)}
         [:li
          {:style {:background-color :white}}
          [:div.layout--space-between
           [suggestion item user-id]]])]]]))

(defn ^:private hangout-who* [{:keys [creator?]} {:keys [hangout invitations]}]
  (let [creator (:creator hangout)
        auth-id (:id @store/user)]
    [:div.layout--inset
     [:ul.layout--stack-between
      (when-not creator?
        [invitation-item (assoc creator :response :creator) false])
      (for [invitation invitations]
        ^{:key (:id invitation)}
        [invitation-item invitation (= auth-id (:user-id invitation))])]]))

(defn ^:private hangout-when* [{:keys [moments]}]
  [hangout-items*
   suggestions/moment-suggestion
   (sort res.suggestions/moment-sorter moments)])

(defn ^:private hangout-where* [{:keys [locations]}]
  [hangout-items*
   suggestions/location-suggestion
   (sort res.suggestions/location-sorter locations)])

(defn ^:private hangout-who [{:keys [creator?] :as attrs} {:keys [hangout] :as state}]
  [:div.layout--stack-between
   [loading/with-resource
    {:state     state
     :resources {:invitations rem.invitations/invitations
                 :associates  rem.users/users}}
    [hangout-who* attrs]]
   (when (or creator? (:others-invite? hangout))
     [suggestions/invitation-form hangout])])

(defn ^:private hangout-when [{:keys [creator?]} {:keys [hangout] :as state}]
  [:div.layout--stack-between
   [loading/with-resource
    {:state     state
     :resources {:moments rem.moments/moments}}
    hangout-when*]
   (when (or creator? (:when-suggestions? hangout))
     [suggestions/moment-form hangout])])

(defn ^:private hangout-where [{:keys [creator?]} {:keys [hangout] :as state}]
  [:div.layout--stack-between
   [loading/with-resource
    {:state     state
     :resources {:locations rem.locations/locations}}
    hangout-where*]
   (when (or creator? (:where-suggestions? hangout))
     [suggestions/location-form hangout])])

(def ^:private section->component
  {:conversation conversations/conversation
   :invitations  hangout-who
   :locations    hangout-where
   :moments      hangout-when})

(defn ^:private hangout-view [attrs state]
  (let [section (get-in state [:page :route-params :section])]
    [:div.layout--space-below.layout--stack-between
     [hangout-header attrs state]
     (when-not (:state attrs)
       [(section->component section) attrs state])]))

(defn ^:private hangout* [state]
  (let [{hangout-id :id :keys [created-by]} (:hangout state)
        creator? (= (:id @store/user) created-by)]
    #?(:cljs (ws/subscribe! [:hangout hangout-id]))
    (r/create-class
      {:component-will-unmount
       (fn [_]
         #?(:cljs (ws/unsubscribe! [:hangout hangout-id])))
       :reagent-render
       (fn [state]
         [fields/stateful nil
          [hangout-view {:creator? creator?} state]])})))

(defn ^:private hangouts* [{:keys [hangouts]}]
  [:div
   [:div.buttons
    [:a.button.is-primary
     {:href (nav/path-for :ui/hangouts.new)}
     "Create"]]
   (if (seq hangouts)
     [:ul
      (for [{:keys [creator id name]} hangouts]
        ^{:key id}
        [:li
         [:a {:href (nav/path-for :ui/hangout {:route-params {:hangout-id id :section :conversation}})}
          name " created by " (:handle creator)]])]
     [:div "You don't have any hangouts, yet. What are you waiting for?"])])

(defn ^:private create* [_state]
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
  [loading/with-resource
   {:state     state
    :resources {:hangouts rem.hangouts/hangouts}}
   hangouts*])

(defn hangout [state]
  [loading/with-resource
   {:state     state
    :resources {:hangout rem.hangouts/hangout}}
   hangout*])

(defn create [state]
  [loading/with-resource
   {:resources {:associates rem.users/users}
    :state     state}
   create*])
