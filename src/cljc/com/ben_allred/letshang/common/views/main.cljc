(ns com.ben-allred.letshang.common.views.main
  #?(:cljs
     (:require
       [com.ben-allred.letshang.ui.services.navigation :as nav])))

(defn not-found [state]
  [:div "not found"])

(defn home [state]
  [:div
   "home"
   [:button.is-primary
    #?(:cljs {:on-click #(nav/go-to! (nav/path-for :auth/logout))})
    "logout"]])
