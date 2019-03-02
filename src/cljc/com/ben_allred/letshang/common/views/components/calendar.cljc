(ns com.ben-allred.letshang.common.views.components.calendar
  (:require
    [clojure.set :as set]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.utils.dom :as dom]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.views.components.core :as components]
    [com.ben-allred.letshang.common.views.components.fields :as fields]
    [com.ben-allred.letshang.common.views.components.loading :as loading]))

(def ^:private day->offset
  {:sunday    0
   :monday    1
   :tuesday   2
   :wednesday 3
   :thursday  4
   :friday    5
   :saturday  6})

(defn ^:private start-of-month [date]
  (let [first-of-month (dates/with date 1 :day)
        day-of-week (dates/day-of-week first-of-month)
        offset (day->offset day-of-week)]
    (cond-> first-of-month
      (pos? offset) (dates/minus offset :days))))

(defn ^:private end-of-month [date]
  (let [last-of-month (dates/plus (dates/with date 1 :day) 1 :months)
        day-of-week (dates/day-of-week last-of-month)
        offset (- 7 (day->offset day-of-week) 1)]
    (cond-> last-of-month
      (not (zero? offset)) (dates/plus offset :days))))

(defn ^:private month-days [date]
  (when (dates/date? date)
    (let [stop-at (dates/plus (end-of-month date) 1 :days)]
      (->> date
           (start-of-month)
           (iterate #(dates/plus % 1 :days))
           (take-while (partial not= stop-at))
           (partition 7)))))

(defn ^:private calendar [attrs]
  (let [current (r/atom (or (:value attrs) (dates/with (dates/today) 1 :day)))]
    (fn [{:keys [on-change value]}]
      (let [month (dates/month @current)]
        [:div.calendar.layout--stack-between
         [:div.calendar-nav.layout--space-between
          [:button.button.is-small.is-white
           {:on-click #(swap! current dates/minus 1 :years)
            :type     :button}
           [components/icon :angle-double-left]]
          [:button.button.is-small.is-white
           {:on-click #(swap! current dates/minus 1 :months)
            :type     :button}
           [components/icon :angle-left]]
          [:span.calendar-display
           {:style {:display :flex :flex-grow 1 :justify-content :center :align-self :center}}
           (dates/month @current) ", " (dates/year @current)]
          [:button.button.is-small.is-white
           {:on-click #(swap! current dates/plus 1 :months)
            :type     :button}
           [components/icon :angle-right]]
          [:button.button.is-small.is-white
           {:on-click #(swap! current dates/plus 1 :years)
            :type     :button}
           [components/icon :angle-double-right]]]
         [:table.table.calendar-table.is-bordered
          [:thead
           [:tr
            (for [day ["Su" "Mo" "Tu" "We" "Th" "Fr" "Sa"]]
              ^{:key day}
              [:th day])]]
          [:tbody.calendar-weeks
           (for [[first-of-week :as week] (month-days @current)]
             ^{:key first-of-week}
             [:tr.calendar-week
              (for [day week
                    :let [selected? (= day value)]]
                ^{:key day}
                [:td
                 {:class [(when (or selected?
                                    (and (not value)
                                         (= day (dates/today))))
                            "is-selected")
                          (when (not= (dates/month day) month)
                            "is-different-month")]}
                 [:button.button.is-white
                  {:type     :button
                   :on-click (fn [_]
                               (when (not selected?)
                                 (reset! current day)
                                 (on-change day)))
                   :class    [(when selected? "is-static")]}
                  (dates/day day)]])])]]]))))

(defn ^:private button [attrs]
  [:button.button
   (-> attrs
       (select-keys #{:class :disabled :on-blur :on-click :ref})
       (assoc :type :button))
   (if-let [value (:value attrs)]
     [:p (dates/format value :default-date)]
     (:button-text attrs "Select…"))
   [:span
    {:style {:margin-left "10px"}}
    [components/icon (if (:open? attrs) :chevron-up :chevron-down)]]])

(defn ^:private picker* [{:keys [button-control loading? open?]
                          :or   {button-control button}
                          :as   attrs}]
  [:div.dropdown
   {:class    [(when open? "is-active")]
    :on-click dom/stop-propagation}
   [:div.dropdown-trigger
    [components/render
     button-control
     (-> attrs
         (set/rename-keys {:on-toggle :on-click})
         (cond-> open? (update :class conj "is-focused")))]]
   (when open?
     [:div.dropdown-menu
      [:div.dropdown-content
       [:div.dropdown-body
        (if loading?
          [loading/spinner]
          [:div.layout--inset
           [calendar attrs]])]]])])

(defn picker [attrs]
  [fields/form-field
   attrs
   [fields/openable [picker* attrs]]])