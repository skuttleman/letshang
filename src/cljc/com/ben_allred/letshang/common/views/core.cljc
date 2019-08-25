(ns com.ben-allred.letshang.common.views.core
  (:require
    [#?(:clj com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [com.ben-allred.letshang.common.utils.logging :as log :include-macros true]
    [com.ben-allred.letshang.common.views.components.loading :as loading]
    [com.ben-allred.letshang.common.views.components.toast :as toast]
    [com.ben-allred.letshang.common.views.pages.dashboard :as dashboard]
    [com.ben-allred.letshang.common.views.pages.hangouts :as hangouts]
    [com.ben-allred.letshang.common.views.pages.main :as main]
    [com.ben-allred.letshang.common.views.pages.sign-up :as sign-up]))

(def ^:private handler->component
  {:ui/home         main/home
   :ui/not-found    main/not-found
   :ui/hangouts     hangouts/hangouts
   :ui/hangouts.new hangouts/create
   :ui/hangout      hangouts/hangout})

(defn ^:private with-layout [component state]
  [:div
   [main/header state]
   [:div.main.layout--inset
    {:class [(str "page-" (name (get-in state [:page :handler])))]}
    [:div.layout--inset
     [component state]]]
   [dashboard/footer]
   [toast/toasts (:toasts state)]])

(defn app [{:keys [auth/user auth/sign-up] :as state}]
  (let [{:keys [handler] :as page} (:page state)
        component (handler->component handler main/not-found)]
    (cond
      (not handler) [loading/spinner {:size :large}]
      user [with-layout component state]
      (not= :ui/home handler) (nav/nav-and-replace! :ui/home {:query-params {:redirect-uri (nav/path-for handler page)}})
      sign-up [sign-up/root state sign-up]
      :else [dashboard/root state])))
