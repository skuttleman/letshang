(ns com.ben-allred.letshang.common.views.core
  (:require
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.logging :as log :include-macros true]
    [com.ben-allred.letshang.common.views.components :as components]
    [com.ben-allred.letshang.common.views.dashboard :as dashboard]
    [com.ben-allred.letshang.common.views.hangouts :as hangouts]
    [com.ben-allred.letshang.common.views.main :as main]))

(def ^:private handler->component
  {:ui/home      main/home
   :ui/not-found main/not-found
   :ui/hangouts  hangouts/hangouts})

(defn ^:private render [component state]
  [:div
   [main/header state]
   [:div.main.inset
    {:class [(str "page-" (name (get-in state [:page :handler])))]}
    [:div.inset
     [component state]]]
   [dashboard/footer]])

(defn app [{:keys [auth/user] :as state}]
  (let [handler (get-in state [:page :handler])
        component (handler->component handler main/not-found)]
    (cond
      (not handler) [components/spinner {:size :large}]
      (env/get :auth/user user) [render component state]
      :else [dashboard/root state])))
