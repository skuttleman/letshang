(ns com.ben-allred.letshang.common.views.hangouts
  (:require
    [com.ben-allred.letshang.common.stubs.actions :as actions]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.views.components :as components]))

(defn ^:private hangout* [{:keys [name creator]}]
  [:li name " created by " (:handle creator)])

(defn ^:private hangouts* [hangouts]
  [:div
   [:ul
    (for [{:keys [id] :as hangout} hangouts]
      ^{:key id} [hangout* hangout])]])

(defn hangouts [state]
  [components/with-status
   {:action  actions/fetch-hangouts
    :tree    hangouts*
    :data-fn :hangouts
    :state   state}])

(defn hangout [{:keys [page] :as state}]
  [components/with-status
   {:action  (actions/fetch-hangout (get-in page [:route-params :hangout-id]))
    :tree    hangout*
    :data-fn :hangout
    :state   state}])

(defn create [state])
