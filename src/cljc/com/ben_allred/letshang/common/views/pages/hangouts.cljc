(ns com.ben-allred.letshang.common.views.pages.hangouts
  (:require
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [clojure.set :as set]
    [com.ben-allred.letshang.common.resources.hangouts :as res.hangouts]
    [com.ben-allred.letshang.common.resources.hangouts.suggestions :as res.suggestions]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.store.actions :as actions]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.strings :as strings]
    [com.ben-allred.letshang.common.utils.users :as users]
    [com.ben-allred.letshang.common.views.components.flip-move :as flip-move]
    [com.ben-allred.letshang.common.views.components.core :as components]
    [com.ben-allred.letshang.common.views.components.dropdown :as dropdown]
    [com.ben-allred.letshang.common.views.components.fields :as fields]
    [com.ben-allred.letshang.common.views.components.form-view :as form-view]
    [com.ben-allred.letshang.common.views.components.loading :as loading]
    [com.ben-allred.letshang.common.views.pages.hangouts.responses :as responses]
    [com.ben-allred.letshang.common.views.pages.hangouts.suggestions :as suggestions]))

(defn ^:private hangout-form [form {:keys [associates invitees-only? on-saved]} & buttons]
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
          (res.hangouts/with-attrs form [:invitation-ids]))])])

(defn ^:private edit-form [hangout _change-state]
  (let [form (res.hangouts/form hangout)]
    (fn [_hangout change-state]
      [:div.layout--space-below
       [hangout-form
        form
        {}
        [:button.button.is-info
         {:type :button :on-click #(change-state nil)}
         "Cancel"]]])))

(defn ^:private invitation-form [{:keys [hangout]}]
  (let [form (res.hangouts/form hangout)
        already-invited? (comp (set (map :id (:invitations hangout))) :id)]
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

(defn ^:private moment-suggestion [{moment-id :id {:keys [positive negative neutral]} :response-counts
                                    :keys [date responses window]} user-id]
  [:<>
   [:div
    {:style {:width "100%"}}
    [components/tooltip
     {:text (str (dates/format date :date/view) ": " (strings/titlize (keywords/safe-name window) " "))}
     (dates/relative date)]]
   [:div.layout--space-between
    (when positive [responses/icon :positive positive])
    (when negative [responses/icon :negative negative])
    (when neutral [responses/icon :neutral neutral])]
   (->> responses
        (colls/find (comp #{user-id} :user-id))
        (:response)
        (assoc {:id moment-id :user-id user-id} :response)
        (conj [responses/form :moment]))])

(defn ^:private location-suggestion [{location-id :id {:keys [positive negative neutral]} :response-counts
                                    :keys [name responses]} user-id]
  [:<>
   [:div
    {:style {:width "100%"}}
    name]
   [:div.layout--space-between
    (when positive [responses/icon :positive positive])
    (when negative [responses/icon :negative negative])
    (when neutral [responses/icon :neutral neutral])]
   (->> responses
        (colls/find (comp #{user-id} :user-id))
        (:response)
        (assoc {:id location-id :user-id user-id} :response)
        (conj [responses/form :location]))])

(defn ^:private hangout-view [{:keys [creator? change-state state]} {:keys [hangout] :as resources}]
  (let [{hangout-id :id :keys [creator invitations locations moments name]} hangout]
    [:div.layout--space-below.layout--stack-between
     (when (not= state :edit)
       [:div.layout--space-between
        [:h1.title.is-5 {:style {:margin-bottom 0}} "Name: "]
        [:span name]
        (when creator?
          [:a
           {:href "#" :on-click #(change-state (when (not= state :edit) :edit))}
           [components/icon :edit]])])
     (when (= state :edit)
       [edit-form hangout change-state])
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
        (when creator?
          [invitation-form resources])])
     [:h2.title.is-6 {:style {:margin-bottom 0}}
      [:a
       {:href "#" :on-click #(change-state (when (not= state :when) :when))}
       [components/icon (if (= state :when) :minus-circle :plus-circle)]
       " When?"]]
     (when (= state :when)
       [:div.layout--stack-between
        [:div.layout--inset
         [:ul.layout--stack-between
          [flip-move/flip-move
           {}
           (for [moment (sort res.suggestions/moment-sorter moments)]
             ^{:key (:id moment)}
             [:li.layout--space-between
              {:style {:background-color :white}}
              [moment-suggestion moment (:id @store/user)]])]]]
        [suggestions/moment hangout-id]])
     [:h2.title.is-6 {:style {:margin-bottom 0}}
      [:a
       {:href "#" :on-click #(change-state (when (not= state :where) :where))}
       [components/icon (if (= state :where) :minus-circle :plus-circle)]
       " Where?"]]
     (when (= state :where)
       [:div.layout--stack-between
        [:div.layout--inset
         [:ul.layout--stack-between
          [flip-move/flip-move
           {}
           (for [location (sort res.suggestions/location-sorter locations)]
             ^{:key (:id location)}
             [:li.layout--space-between
              {:style {:background-color :white}}
              [location-suggestion location (:id @store/user)]])]]]
        [suggestions/location hangout-id]])]))

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
         :on-saved res.hangouts/create->modify}
        (if (not (forms/ready? form))
          [:button.button.is-warning
           {:disabled true :type :button}
           "Cancel"]
          [:a.button.is-warning
           {:href (nav/path-for :ui/hangouts)}
           "Cancel"])]])))

(defn hangouts [state]
  [loading/with-status
   {:action actions/fetch-hangouts
    :keys   #{:hangouts}
    :state  state}
   hangouts*])

(defn hangout [{:keys [page] :as state}]
  [loading/with-status
   {:action (actions/combine (actions/fetch-hangout (get-in page [:route-params :hangout-id]))
                             actions/fetch-associates)
    :keys   #{:hangout :associates}
    :state  state}
   hangout*])

(defn create [state]
  [loading/with-status
   {:action actions/fetch-associates
    :keys   #{:associates}
    :state  state}
   create*])
