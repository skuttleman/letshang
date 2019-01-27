(ns com.ben-allred.letshang.common.views.dashboard
  #?(:cljs
     (:require
       [com.ben-allred.letshang.ui.services.navigation :as nav]
       [com.ben-allred.letshang.common.utils.logging :as log])))

(defn root [state]
  [:div
   "dashboard"
   [:button.is-primary
    #?(:cljs {:on-click #(nav/go-to! (log/spy (nav/path-for :auth/login {:query-params {:email "skuttleman@gmail.com"}})))})
    "login"]])
