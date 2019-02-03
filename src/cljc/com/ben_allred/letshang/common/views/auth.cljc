(ns com.ben-allred.letshang.common.views.auth
  (:require
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]))

(defn login [text email]
  [#?(:clj :a :cljs :button)
   {:class ["button" "is-primary"]
    #?@(:clj  [:href (nav/path-for :auth/login {:query-params {:email email}})]
        :cljs [:on-click #(nav/go-to! (nav/path-for :auth/login {:query-params {:email email}}))])}
   text])

(defn logout [{:keys [text minimal? class]}]
  [:a
   (cond-> {:class class
            #?@(:clj  [:href (nav/path-for :auth/logout)]
                :cljs [:href "#"
                       :on-click #(nav/go-to! (nav/path-for :auth/logout))])}
     (not minimal?) (update :class concat ["button" "is-primary"]))
   text])
