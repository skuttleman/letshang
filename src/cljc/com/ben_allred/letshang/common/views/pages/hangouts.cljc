(ns com.ben-allred.letshang.common.views.pages.hangouts
  (:require
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.stubs.actions :as actions]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.dom :as dom]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.users :as users]
    [com.ben-allred.letshang.common.views.components.dropdown :as dropdown]
    [com.ben-allred.letshang.common.views.components.fields :as fields]
    [com.ben-allred.letshang.common.views.components.loading :as loading]
    [com.ben-allred.letshang.common.views.resources.core :as res]
    [com.ben-allred.letshang.common.views.resources.hangouts :as hangouts.res]))

(defn hangout-form [form associates]
  [:form.form
   {:on-submit (fn [e]
                 (dom/prevent-default e)
                 (-> form
                     (forms/persist!)
                     (ch/then hangouts.res/create->modify)
                     (ch/catch res/toast-error)))}
   [fields/input
    (-> {:label "Name"}
        (hangouts.res/with-attrs form [:name]))]
   [dropdown/dropdown
    (-> {:label   "Invitees"
         :options (map (juxt :id users/full-name) associates)}
        (hangouts.res/with-attrs form [:invitee-ids]))]
   [:button.button.is-primary
    {:type     :submit
     :disabled (or (not (forms/ready? form))
                   (and (forms/attempted? form) (not (forms/valid? form))))}
    "Save"]])

(defn ^:private creator's-hangout-view [{:keys [change-state]} {{:keys [name invitees]} :hangout}]
  [:div
   [:nav.buttons
    [:button.button.is-info
     {:on-click #(change-state :edit)}
     "Edit"]]
   [:h1.label name]
   [:ul
    (for [invitee invitees]
      ^{:key (:id invitee)}
      [:li (:handle invitee)])]])

(defn ^:private creator's-hangout-edit [_attrs {:keys [hangout]}]
  (let [form (hangouts.res/form hangout)
        already-invited? (comp (set (map :id (:invitees hangout))) :id)]
    (fn [{:keys [change-state]} {:keys [associates]}]
      [:div
       [:nav.buttons
        [:button.button.is-info
         {:on-click #(change-state :normal)}
         "Cancel"]]
       [hangout-form form (remove already-invited? associates)]])))

(defn ^:private creator's-hangout [attrs resources]
  (case (:state attrs)
    :normal [creator's-hangout-view attrs resources]
    :edit [creator's-hangout-edit attrs resources]))

(defn ^:private invitee's-hangout [user {{:keys [name invitees]} :hangout}]
  [:div
   [:h1.label name]
   [:ul
    (for [invitee invitees]
      ^{:key (:id invitee)}
      [:li (:handle invitee)])]])

(defn ^:private hangout* [user resources]
  (if (= (:id user) (get-in resources [:hangout :created-by]))
    [fields/stateful :normal [creator's-hangout {} resources]]
    [invitee's-hangout user resources]))

(defn ^:private hangouts* [{:keys [hangouts]}]
  [:div
   [:nav.buttons
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
  (let [form (hangouts.res/form)]
    (fn [{:keys [associates]}]
      [:div
       [:nav.buttons
        (if (not (forms/ready? form))
          [:button.button.is-warning.is-text
           {:disabled true}
           "Cancel"]
          [:a.button.is-warning.is-text
           {:href (nav/path-for :ui/hangouts)}
           "Cancel"])]
       [hangout-form form associates]])))

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
   [hangout* (:auth/user state)]])

(defn create [state]
  [loading/with-status
   {:action actions/fetch-associates
    :keys   #{:associates}
    :state  state}
   create*])
