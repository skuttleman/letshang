(ns com.ben-allred.letshang.common.views.components.fields
  (:require
    [com.ben-allred.letshang.common.services.transformers :as transformers]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.dom :as dom]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.views.components.core :as components]))

(defn ^:private modify-coll [xform coll]
  (transduce (comp (map-indexed vector) xform) conj coll))

(defn ^:private update-by-idx [idx value coll]
  (modify-coll (map (fn [[idx' value']]
                      (if (= idx idx')
                        value
                        value')))
               coll))

(defn ^:private remove-by-idx [idx coll]
  (modify-coll (comp (remove (comp #{idx} first)) (map second)) coll))

(defn form-field [{:keys [attempted? errors label visited?]} & body]
  (let [errors (seq (remove nil? errors))
        show-errors? (and errors (or visited? attempted?))]
    [:div.form-field
     {:class [(when show-errors? "errors")]}
     [:div
      (when label [:label.label label])
      (into [:div.form-field-control] body)
      (when show-errors?
        [:ul.error-list
         (for [error errors]
           [:li.error
            {:key error}
            error])])]]))

(defn ^:private with-auto-focus [component]
  (fn [{:keys [auto-focus?]} & _]
    (let [node-atom (atom nil)
          ref (fn [node] (some->> node (reset! node-atom)))]
      (r/create-class
        {:component-did-update
         (fn [this _]
           (when-let [node @node-atom]
             (when (and auto-focus? (not (:disabled (second (r/argv this)))))
               (reset! node-atom nil)
               (dom/focus node))))
         :reagent-render
         (fn [attrs & args]
           (into [component (cond-> (dissoc attrs :auto-focus?)
                              auto-focus? (assoc :ref ref))]
                 args))}))))

(def ^{:arglists '([attrs options])} select
  (with-auto-focus
    (fn [{:keys [disabled on-change value] :as attrs} options]
      (let [option-values (set (map first options))
            value (if (contains? option-values value)
                    value
                    ::empty)]
        [form-field
         attrs
         [:select.select
          (-> {:value    (str value)
               :disabled #?(:clj true :cljs disabled)
               #?@(:cljs [:on-change (comp on-change
                                           (into {} (map (juxt str identity) option-values))
                                           dom/target-value)])}
              (merge (select-keys attrs #{:class :on-blur :ref})))
          (for [[option label attrs] (cond->> options
                                       (= ::empty value) (cons [::empty
                                                                (str "Choose" #?(:clj "..." :cljs "…"))
                                                                {:disabled true}]))
                :let [str-option (str option)]]
            [:option
             (assoc attrs :value str-option :key str-option #?@(:clj [:selected (= option value)]))
             label])]]))))

(def ^{:arglists '([attrs])} textarea
  (with-auto-focus
    (fn [{:keys [disabled on-change value] :as attrs}]
      [form-field
       attrs
       [:textarea.textarea
        (-> {:value    value
             :disabled #?(:clj true :cljs disabled)
             #?@(:cljs [:on-change (comp on-change dom/target-value)])}
            (merge (select-keys attrs #{:class :on-blur :ref})))
        #?(:clj value)]])))

(def ^{:arglists '([attrs])} input
  (with-auto-focus
    (fn [{:keys [disabled on-change value type] :as attrs}]
      [form-field
       attrs
       [:input.input
        (-> {:value    value
             :type     (or type :text)
             :disabled #?(:clj true :cljs disabled)
             #?@(:cljs [:on-change (comp on-change dom/target-value)])}
            (merge (select-keys attrs #{:class :on-blur :ref})))]])))

(defn phone-number [attrs]
  [input (-> attrs
             (update :value transformers/phone->view)
             (update :on-change comp transformers/phone->model))])

(defn button-group [{:keys [class disabled on-change value] :as attrs} options]
  [form-field
   attrs
   [:ul.button-group
    (cond-> {:class class}
      disabled (update :class conj "is-disabled"))
    (for [[option display] options
          :let [active? (= value option)]]
      ^{:key option}
      [:li.grouped-button
       (-> attrs
           (assoc :class [(when active? "is-selected")])
           (dissoc :button-class :label :on-change :value)
           (cond->
             (and (not active?) (not disabled))
             (assoc :on-click #(on-change option))))
       display])]])

(defn openable [_component]
  (let [open? (r/atom false)
        listeners [(dom/add-listener dom/window "click" #(reset! open? false))
                   (dom/add-listener dom/window "keypress" #(when (= :esc (dom/event->key %))
                                                              (reset! open? false)))]]
    (r/create-class
      {:component-will-unmount
       (fn [_]
         (run! dom/remove-listener listeners))
       :reagent-render
       (fn [component]
         (let [attrs {:on-toggle (fn [e]
                                   (dom/stop-propagation e)
                                   (swap! open? not))
                      :open?     @open?}]
           (components/render-with-attrs component attrs)))})))

(defn stateful [initial-state _component]
  (let [state (r/atom initial-state)
        change-state (partial reset! state)]
    (fn [_initial-state component]
      (let [attrs {:state        @state
                   :change-state change-state}]
        (components/render-with-attrs component attrs)))))

(defn multi [{:keys [key-fn value new-fn on-change errors class] :as attrs} component]
  [form-field
   (dissoc attrs :errors)
   [:div
    [:button.button.is-small.add-item
     {:type :button
      #?@(:clj  [:disabled true]
          :cljs [:on-click #(on-change (conj value (new-fn (count value))))])}
     [:i.fa.fa-plus]]]
   [:ul.multi
    {:class class}
    (for [[idx val :as key] (map-indexed vector value)]
      [:li.multi-item
       {:key (key-fn key)}
       [:div
        [:button.button.is-small.remove-item
         {:type :button
          #?@(:clj  [:disabled true]
              :cljs [:on-click #(on-change (remove-by-idx idx value))])}
         [:i.fa.fa-minus.remove-item]]]
       [component (-> attrs
                      (dissoc :label)
                      (assoc :value val
                             :errors (nth errors idx nil)
                             #?@(:cljs [:on-change #(on-change (update-by-idx idx % value))])))]])]])
