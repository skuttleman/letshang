(ns com.ben-allred.letshang.common.views.auth
  (:require
    [#?(:clj  com.ben-allred.letshang.api.services.navigation
        :cljs com.ben-allred.letshang.ui.services.navigation) :as nav]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.strings :as strings]))

(defn login [text email redirect-uri]
  [#?(:clj :a :cljs :button)
   {:class ["button" "is-primary"]
    #?@(:clj  [:href (nav/path-for :auth/login {:query-params {:email email :redirect-uri redirect-uri}})]
        :cljs [:on-click #(nav/go-to! (nav/path-for :auth/login {:query-params {:email email :redirect-uri redirect-uri}}))])}
   text])

(defn login-as [_text _redirect-uri]
  (let [email (r/atom nil)]
    (fn [text redirect-uri]
      [:div
       [:input {:type      :text
                :value     @email
                :on-change #(reset! email (strings/trim-to-nil (.-value (.-target %))))
                #?@(:clj [:disabled true])}]
       [login text @email redirect-uri]])))

(defn logout [{:keys [text minimal? class]}]
  [:a
   (cond-> {:class class
            #?@(:clj  [:href (nav/path-for :auth/logout)]
                :cljs [:href "#"
                       :on-click #(nav/go-to! (nav/path-for :auth/logout))])}
     (not minimal?) (update :class concat ["button" "is-primary"]))
   text])
