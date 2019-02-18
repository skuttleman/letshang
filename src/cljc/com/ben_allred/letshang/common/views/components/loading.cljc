(ns com.ben-allred.letshang.common.views.components.loading
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.stubs.store :as store]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.common.views.components.core :as components]))

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

(defn spinner [{:keys [size]}]
  [:div.loader
   {:class [(keywords/safe-name size)]}])

(defn with-status [{:keys [action]} _control]
  (let [finished? (r/atom false)]
    (async/go
      (async/<! (store/dispatch action))
      (reset! finished? true))
    (fn [{:keys [keys state]} control]
      (let [resources (select-keys state keys)
            resource-vals (vals resources)
            statuses (set (map first resource-vals))
            messages (->> resource-vals
                          (filter (comp #{:error} first))
                          (keep (comp :message second)))]
        (cond
          (and @finished? (contains? statuses :error))
          [components/alert :error [status-messages messages]]

          (and @finished? (= #{:success} statuses))
          [components/render control (maps/map-vals second resources)]

          :else
          [:div.layout--center-content [spinner {:size :large}]])))))
