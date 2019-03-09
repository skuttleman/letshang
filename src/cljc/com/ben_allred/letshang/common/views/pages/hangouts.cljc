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

(defn ^:private hangout-form [form associates on-saved & buttons]
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
          (res.hangouts/with-attrs form [:invitation-ids]))])])

(defn ^:private invitation-item [{:keys [handle response] :as invitation} current-user?]
  [:li.layout--space-between.layout--align-center
   [:em handle]
   (if current-user?
     [responses/form :invitation (set/rename-keys invitation {:invitation-id :id})]
     [responses/icon response])])

(defn ^:private moment-suggestion [{moment-id :id {:keys [positive negative neutral]} :response-counts
                                    :keys [date responses] :as moment} user-id]
  [:<>
   [:div
    {:style {:width "100%"}}
    [components/tooltip
     {:text (str (dates/format date :date/view)
                 ": "
                 (strings/titlize (keywords/safe-name (:window moment)) " "))}
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

(defn ^:private creator's-hangout-view [{:keys [change-state]} {{hangout-id :id :keys [invitations moments name]} :hangout}]
  [:div.layout--space-below
   [:div.buttons
    [:button.button.is-info
     {:on-click #(change-state :edit)}
     "Edit"]]
   [:h1.label name]
   [:h2.label "Who's coming?"]
   [:div.layout--inset
    [:ul.layout--stack-between
     (for [invitation invitations]
       ^{:key (:id invitation)}
       [invitation-item invitation false])]]
   [:h2.label "When?"]
   [:div.layout--stack-between
    [:div.layout--inset
     [:ul.layout--stack-between
      [flip-move/flip-move
       {}
       (for [moment (sort res.suggestions/moment-sorter moments)]
         ^{:key (:id moment)}
         [:li.layout--space-between
          [moment-suggestion moment (:id @store/user)]])]]]
    [suggestions/moment hangout-id]]
   [:h2.label "Where?"]])

(defn ^:private creator's-hangout-edit [_attrs {:keys [hangout]}]
  (let [form (res.hangouts/form hangout)
        already-invited? (comp (set (map :id (:invitations hangout))) :id)]
    (fn [{:keys [change-state]} {:keys [associates]}]
      [:div.layout--space-below
       [hangout-form
        form
        (remove already-invited? associates)
        (res.hangouts/on-modify change-state)
        [:button.button.is-info
         {:on-click #(change-state :normal)
          :type     :button}
         "Cancel"]]])))

(defn ^:private creator's-hangout [attrs resources]
  (case (:state attrs)
    :normal [creator's-hangout-view attrs resources]
    :edit [creator's-hangout-edit attrs resources]))

(defn ^:private invitee's-hangout [{{hangout-id :id :keys [creator name moments invitations]} :hangout}]
  [:div.layout--space-below
   [:h1.is-size-3 name]
   [:label.label "Invitees"]
   [:ul.layout--stack-between
    [invitation-item (assoc creator :response :creator) false]
    (for [{invitation-id :id :as invitation} invitations]
      ^{:key invitation-id}
      [invitation-item invitation (= invitation-id (:id @store/user))])]
   [:h2.label "When?"]
   [:div.layout--stack-between
    [:div.layout--inset
     [:ul.layout--stack-between
      [flip-move/flip-move
       {}
       (for [moment (sort res.suggestions/moment-sorter moments)]
         ^{:key (:id moment)}
         [:li.layout--space-between
          [moment-suggestion moment (:id @store/user)]])]]]
    [suggestions/moment hangout-id]]])

(defn ^:private hangout* [resources]
  (if (= (:id @store/user) (get-in resources [:hangout :created-by]))
    [fields/stateful :normal [creator's-hangout {} resources]]
    [invitee's-hangout resources]))

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
        associates
        res.hangouts/create->modify
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
