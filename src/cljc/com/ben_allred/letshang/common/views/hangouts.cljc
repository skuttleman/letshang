(ns com.ben-allred.letshang.common.views.hangouts
  (:require
    [com.ben-allred.letshang.common.stubs.actions :as actions]
    [com.ben-allred.letshang.common.views.components :as components]))

(defn ^:private hangouts* [hangouts]
  [:div
   [:ul
    (for [{:keys [name creator id]} hangouts]
      ^{:key id} [:li name " created by " (:handle creator)])]])

(defn hangouts [state]
  [components/with-status
   {:action  actions/fetch-hangouts
    :tree    hangouts*
    :data-fn :hangouts
    :state   state}])
