(ns com.ben-allred.letshang.common.views.core
  (:require
    [com.ben-allred.letshang.common.utils.logging :as log :include-macros true]
    [com.ben-allred.letshang.common.views.components.loading :as loading]
    [com.ben-allred.letshang.common.views.components.toast :as toast]
    [com.ben-allred.letshang.common.views.pages.dashboard :as dashboard]
    [com.ben-allred.letshang.common.views.pages.hangouts :as hangouts]
    [com.ben-allred.letshang.common.views.pages.main :as main]
    [com.ben-allred.letshang.common.views.pages.sign-up :as sign-up]))

(def ^:private handler->component
  {:ui/home        main/home
   :ui/not-found   main/not-found
   :ui/hangouts    hangouts/hangouts
   :ui/hangout-new hangouts/create
   :ui/hangout     hangouts/hangout})

(defn ^:private with-layout [component state]
  [:div
   [main/header state]
   [:div.main.layout--inset
    {:class [(str "page-" (name (get-in state [:page :handler])))]}
    [:div.layout--inset
     [component state]]]
   [dashboard/footer]
   [toast/toasts (:toasts state)]])

(defn app [{:keys [auth/sign-up] :as state}]
  (let [handler (get-in state [:page :handler])
        component (handler->component handler main/not-found)]
    (cond
      (not handler) [loading/spinner {:size :large}]
      (:auth/user state) [with-layout component state]
      sign-up [sign-up/root state sign-up]
      :else [dashboard/root state])))
