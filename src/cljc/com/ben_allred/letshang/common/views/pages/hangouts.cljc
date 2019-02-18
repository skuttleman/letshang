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

(defn ^:private hangout-form [form associates on-saved & buttons]
  [:form.form.layout--stack
   {:on-submit (fn [e]
                 (dom/prevent-default e)
                 (-> form
                     (forms/persist!)
                     (ch/peek (res/toast-success "Your hangout has been saved.")
                              res/toast-error)
                     (ch/then on-saved)))}
   [fields/input
    (-> {:label "Name"}
        (hangouts.res/with-attrs form [:name]))]
   (when (seq associates)
     [dropdown/dropdown
      (-> {:label   "Invitees"
           :options (map (juxt :id users/full-name) associates)}
          (hangouts.res/with-attrs form [:invitee-ids]))])
   (-> [:div.buttons
        [:button.button.is-primary
         {:type     :submit
          :disabled (or (not (forms/ready? form))
                        (and (forms/attempted? form) (not (forms/valid? form))))}
         "Save"]]
       (into buttons)
       (cond->
         (not (forms/ready? form))
         (conj [:div {:style {:margin-bottom "8px"}} [loading/spinner]])))])

(defn ^:private creator's-hangout-view [{:keys [change-state]} {{:keys [name invitees]} :hangout}]
  [:div
   [:div.buttons
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
       [hangout-form
        form
        (remove already-invited? associates)
        (hangouts.res/on-modify change-state)
        [:button.button.is-info
         {:on-click #(change-state :normal)
          :type :button}
         "Cancel"]]])))

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
  (let [form (hangouts.res/form)]
    (fn [{:keys [associates]}]
      [:div
       [hangout-form
        form
        associates
        hangouts.res/create->modify
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
   [hangout* (:auth/user state)]])

(defn create [state]
  [loading/with-status
   {:action actions/fetch-associates
    :keys   #{:associates}
    :state  state}
   create*])
