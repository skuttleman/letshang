(ns com.ben-allred.letshang.common.views.components.dropdown
  (:require
    [clojure.set :as set]
    [com.ben-allred.letshang.common.utils.dom :as dom]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.common.views.components.core :as components]
    [com.ben-allred.letshang.common.views.components.fields :as fields]
    [com.ben-allred.letshang.common.views.components.loading :as loading]))

(defn lazy-list [{:keys [value options]}]
  (let [options (concat (filter (comp (partial contains? value) first) options)
                        (remove (comp (partial contains? value) first) options))]
    (fn [{:keys [item-control on-change value]}]
      [:ul.dropdown-items.lazy-list
       (for [[id display] options
             :let [selected? (contains? value id)]]
         ^{:key id} [:li.dropdown-item.pointer
                     {:class    [(when selected? "is-active")]
                      :on-click (fn [e]
                                  (dom/stop-propagation e)
                                  (-> (if (contains? value id)
                                        (disj value id)
                                        ((fnil conj #{}) value id))
                                      (on-change)))}
                     [components/render item-control display]])])))

(defn button [attrs]
  (let [selected-count (count (:selected attrs))]
    [:button.button
     (-> attrs
         (select-keys #{:class :disabled :on-blur :on-click :ref})
         (assoc :type :button))
     (case selected-count
       0 "Select…"
       1 "1 Item Selected"
       (str selected-count " Items Selected"))
     [:span
      {:style {:margin-left "10px"}}
      [components/icon (if (:open? attrs) :chevron-up :chevron-down)]]]))

(defn ^:private dropdown* [{:keys [button-control loading? list-control on-search open? options options-by-id value]
                            :or   {list-control lazy-list button-control button}
                            :as   attrs}]
  (let [selected (seq (map options-by-id value))]
    [:div.dropdown
     {:class [(when open? "is-active")]}
     [:div.dropdown-trigger
      [components/render
       button-control
       (-> attrs
           (set/rename-keys {:on-toggle :on-click})
           (cond-> selected (assoc :selected selected)))]]
     (when open?
       [:div.dropdown-menu
        [:div.dropdown-content
         (when on-search
           [:div.dropdown-search
            [fields/input {:on-change on-search}]])
         [:div.dropdown-body
          (cond
            loading?
            [loading/spinner]

            (seq options)
            [list-control attrs]

            :else
            [components/alert :info "No results"])]]])]))

(defn dropdown [{:keys [options] :as attrs}]
  (let [options-by-id (or (:options-by-id attrs) (maps/select-by :id options))]
    [fields/form-field
     attrs
     [fields/openable [dropdown* (assoc attrs :options-by-id options-by-id)]]]))

(defn singleable [{:keys [value] :as attrs}]
  (let [value (if (nil? value) #{} #{value})]
    (-> attrs
        (assoc :value value)
        (update :on-change comp #(->> % (remove value) (first))))))
