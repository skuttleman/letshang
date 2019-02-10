(ns com.ben-allred.letshang.common.views.hangouts
  (:require
    #?(:cljs [com.ben-allred.letshang.ui.services.forms.standard :as forms.std])
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]

    [clojure.pprint :as pp]

    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.forms.noop :as forms.noop]
    [com.ben-allred.letshang.common.stubs.actions :as actions]
    [com.ben-allred.letshang.common.stubs.store :as store]
    [com.ben-allred.letshang.common.templates.fields :as fields]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.dom :as dom]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.strings :as strings]
    [com.ben-allred.letshang.common.utils.users :as users]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]
    [com.ben-allred.letshang.common.views.components :as components]))

(defn ^:private create-api [[dispatch]]
  (reify
    forms/IFetch
    (fetch [_]
      (ch/resolve nil))
    forms/ISave
    (save! [_ model]
      (dispatch (actions/create-hangout model)))))

(def ^:private model->view
  {:name str})

(def ^:private view->model
  {:name strings/empty-to-nil})

(defn ^:private hangout* [{{:keys [name invitees]} :hangout :keys [associates]}]
  [:div
   [:h1.label name]
   [:ul
    (for [invitee invitees]
      ^{:key (:id invitee)}
      [:li (:handle invitee)])]
   [:p "The people you know"]])

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

(defn ^:private create* [resources]
  (let [form #?(:clj  (forms.noop/create nil)
                :cljs (forms.std/form
                        (forms.std/resource (store/dispatch create-api))
                        identity
                        (partial hash-map :data)
                        (constantly nil))
                :default nil)]
    (fn [{:keys [associates]}]
      [:div
       [:nav.buttons
        [:a.button.is-warning.is-text
         {:href (nav/path-for :ui/hangouts)}
         "Cancel"]]
       [:pre {:style {:font-family :monospace}}
        (with-out-str (pp/pprint @form))]
       [:form
        {:on-submit (comp (fn [e]
                            (dom/prevent-default e)
                            #_(-> form
                                (forms/persist!)
                                (ch/then (fn [response]
                                           #?(:cljs
                                              (nav/nav-and-replace! :ui/hangout
                                                                    {:route-params {:hangout-id (get-in response [:data :id])}})))))))}
        [fields/input
         (-> {:label "Name"}
             (forms/with-attrs form [:name] model->view view->model))]
        [:button.button.is-primary
         {:type :submit}
         "Submit"]]])))

(defn hangouts [state]
  [components/with-status
   {:action actions/fetch-hangouts
    :keys   #{:hangouts}
    :state  state}
   hangouts*])

(defn hangout [{:keys [page] :as state}]
  [components/with-status
   {:action (actions/combine (actions/fetch-hangout (get-in page [:route-params :hangout-id]))
                             actions/fetch-associates)
    :keys   #{:hangout :associates}
    :state  state}
   hangout*])

(defn create [state]
  [components/with-status
   {:action actions/fetch-associates
    :keys   #{:associates}
    :state  state}
   create*])
