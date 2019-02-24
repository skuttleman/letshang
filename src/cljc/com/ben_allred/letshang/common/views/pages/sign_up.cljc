(ns com.ben-allred.letshang.common.views.pages.sign-up
  (:require
    #?(:cljs [com.ben-allred.letshang.ui.services.forms.standard :as forms.std])
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [clojure.string :as string]
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.forms.noop :as forms.noop]
    [com.ben-allred.letshang.common.services.store.actions :as actions]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.services.validators :as validators]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.views.components.fields :as fields]
    [com.ben-allred.letshang.common.views.components.form-view :as form-view]
    [com.ben-allred.letshang.common.views.components.toast :as toast]
    [com.ben-allred.letshang.common.views.pages.dashboard :as dashboard]
    [com.ben-allred.letshang.common.views.resources.core :as res]
    [com.ben-allred.letshang.common.views.auth :as auth]))

(def ^:private model->source
  (partial hash-map :data))

(defn ^:private api [user]
  (reify
    forms/IFetch
    (fetch [_]
      (ch/resolve user))
    forms/ISave
    (save! [_ model]
      (-> model
          (model->source)
          (actions/register-user)
          (store/dispatch)
          (ch/peek (fn [_]
                     (nav/go-to! (nav/path-for :auth/login {:query-params (select-keys model #{:email})})))
                   (res/toast-error "Something went wrong."))))))

(def ^:private validator
  (f/validator
    {:email         [(f/required "Email is required")
                     (f/pred validators/email? "Not a valid email")]
     :handle        [(f/required "Handle is required")
                     (f/identity string/lower-case "Handle must be lower-case")]
     :first-name    (f/required "First name is required")
     :last-name     (f/required "Last name is required")
     :mobile-number [(f/required "Mobile number is required")
                     (f/pred validators/phone-number? "Not a valid phone number")]}))

(def ^:private model->view
  {})

(def ^:private view->model
  {:email         not-empty
   :handle        (comp not-empty #(when % (string/lower-case %)))
   :first-name    not-empty
   :last-name     not-empty
   :mobile-number not-empty})

(defn ^:private with-attrs [attrs form path]
  (forms/with-attrs attrs form path model->view view->model))

(defn ^:private sign-up* [new-user]
  (let [form #?(:cljs    (forms.std/create (api new-user) validator)
                :default (forms.noop/create new-user))]
    (fn [_new-user]
      [form-view/form
       {:form form}
       [fields/input
        (-> {:label "Screen name"}
            (with-attrs form [:handle]))]
       [fields/input
        (-> {:label "First name"}
            (with-attrs form [:first-name]))]
       [fields/input
        (-> {:label "Last name"}
            (with-attrs form [:last-name]))]
       [fields/input
        (-> {:label "Email"}
            (with-attrs form [:email]))]
       [fields/phone-number
        (-> {:label "Mobile phone number"}
            (with-attrs form [:mobile-number]))]])))

(defn ^:private sign-up-form [new-user]
  [:div.sign-up-content.gutters.layout--xl.layout--xxl.layout--inset
   [:div.layout--space-above
    {:style {:display :flex :justify-content :flex-end}}
    [auth/logout {:text "logout"}]]
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
