(ns com.ben-allred.letshang.common.views.core
  (:require
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.templates.core :as templates]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.views.components :as components]
    [com.ben-allred.letshang.common.views.dashboard :as dashboard]
    [com.ben-allred.letshang.common.views.main :as main]))

(def ^:private handler->component
  {:ui/home      main/home
   :ui/not-found main/not-found})

(defn ^:private render [component state]
  [:div
   [:header "header"]
   [:div
    (templates/classes {(str "page-" (get-in state [:page :handler])) true})
    [component state]]
   [:footer "footer"]])

(defn app [{:keys [auth/user] :as state}]
  (let [handler (get-in state [:page :handler])
        component (handler->component handler main/not-found)]
    (cond
      (not handler) [components/spinner]
      (log/spy (env/get :auth/user user)) [render component state]
      :else [dashboard/root state])))
