(ns com.ben-allred.letshang.common.views.main
  (:require
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.strings :as strings]
    [com.ben-allred.letshang.common.utils.users :as users]
    [com.ben-allred.letshang.common.views.auth :as auth]))

(defn not-found [_state]
  [:div "page not found"])

(defn header [_state]
  (let [shown? (r/atom false)]
    (fn [_state]
      [:header.header
       [:nav.navbar
        {:role "navigation" :aria-label "main navigation"}
        [:div.navbar-brand
         [:a.navbar-item {:href (nav/path-for :ui/home)}
          [:img {:src "/images/logo.png" :height 48 :width 48}]]]
        [:div.navbar-start
         {:style {:position :relative}}
         [:span.navbar-burger.burger
          {:on-click #(swap! shown? not) :cursor :pointer}
          [:span {:aria-hidden "true"}]
          [:span {:aria-hidden "true"}]
          [:span {:aria-hidden "true"}]]
         [:div#header-nav.navbar-menu
          (cond-> {:class [(when @shown? "expanded")]}
            @shown? (assoc :on-click #(reset! shown? false)))
          [:ul.navbar-start
           [:li
            [:a.navbar-item {:href (nav/path-for :ui/home)} "Home"]]
           [:li
            [:a.navbar-item {:href (nav/path-for :ui/hangouts)} "My Hangouts"]]
           [:li.undersize
            [:hr.nav-divider]]
           [:li.undersize
            [auth/logout {:text "Logout"
                          :minimal? true
                          :class ["navbar-item"]}]]]]]
        [:div.navbar-end.oversize
         [:div.navbar-item
          [:div.buttons
           [auth/logout {:text "logout"}]]]]]])))

(defn home [state]
  [:p.has-text-centered
   (->> state
        (:auth/user)
        (users/full-name)
        (strings/format "Hi, %s. Thanks for coming to hang out."))])
