(ns com.ben-allred.letshang.common.views.components.loading
  (:require
    [com.ben-allred.letshang.common.resources.remotes.core :as remotes]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.common.views.components.core :as components]
    [com.ben-allred.vow.core :as v]))

(defn ^:private status-messages [messages]
  [:div
   (if (seq messages)
     [:<>
      [:p "The following errors occurred:"]
      [:ul.bullets
       (for [[msg count] (frequencies messages)]
         ^{:key msg} [:li msg (when (> count 1) (str " (" count ")"))])]]
     [:p "An unknown error occurred."])
   [:p "Please try again."]])

(defn spinner
  ([]
   (spinner nil))
  ([{:keys [size]}]
   [:div.loader
    {:class [(keywords/safe-name size)]}]))

(defn with-resource [{:keys [resources]} _control]
  (r/create-class
    {:component-did-mount
     (fn [_]
       (run! (comp deref val) resources))
     :component-will-unmount
     (fn [_]
       (run! remotes/invalidate! (vals resources)))
     :reagent-render
     (fn [{:keys [state] :as attrs} control]
       (let [resource-vals (vals resources)
             ready? (every? remotes/ready? resource-vals)
             success? (every? remotes/success? resource-vals)]
         (cond
           (and ready? (not success?))
           [components/alert :error [status-messages (->> resource-vals
                                                          (remove remotes/success?)
                                                          (map (comp :message deref)))]]

           ready?
           [components/render control (merge state (maps/map-vals deref resources))]

           :else
           [:div.layout--center-content [spinner {:size (:size attrs :large)}]])))}))
