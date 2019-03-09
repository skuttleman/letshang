(ns com.ben-allred.letshang.common.views.pages.sign-up
  (:require
    [com.ben-allred.letshang.common.resources.sign-up :as sign-up.res]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.views.auth :as auth]
    [com.ben-allred.letshang.common.views.components.fields :as fields]
    [com.ben-allred.letshang.common.views.components.form-view :as form-view]
    [com.ben-allred.letshang.common.views.components.toast :as toast]
    [com.ben-allred.letshang.common.views.pages.dashboard :as dashboard]))

(defn ^:private sign-up* [new-user]
  (let [form (sign-up.res/form new-user)]
    (fn [_new-user]
      [form-view/form
       {:form form}
       [fields/input
        (-> {:label "Email"
             :disabled true}
            (sign-up.res/with-attrs form [:email]))]
       [fields/input
        (-> {:label "Screen name"
             :auto-focus? true}
            (sign-up.res/with-attrs form [:handle]))]
       [fields/input
        (-> {:label "First name"}
            (sign-up.res/with-attrs form [:first-name]))]
       [fields/input
        (-> {:label "Last name"}
            (sign-up.res/with-attrs form [:last-name]))]
       [fields/phone-number
        (-> {:label "Mobile phone number"}
            (sign-up.res/with-attrs form [:mobile-number]))]])))

(defn ^:private sign-up-form [new-user]
  [:div.sign-up-content.gutters.layout--xl.layout--xxl.layout--inset
   [:div.layout--space-above
    {:style {:display :flex :justify-content :flex-end}}
    [auth/logout {:text "start over"}]]
   [:p.has-text-centered
    {:style {:font-weight :bold}}
    "Thanks for coming to hang out."]
   [:p.has-text-centered "Fill out your profile information to get started."]
   [:div.gutters.layout--xxl
    [:div.layout--space-below
     [sign-up* new-user]]]])

(defn root [state new-user]
  [:div.page-dashboard.page-sign-up
   [dashboard/jumbotron false]
   [sign-up-form new-user]
   [dashboard/footer]
   [toast/toasts (:toasts state)]])
