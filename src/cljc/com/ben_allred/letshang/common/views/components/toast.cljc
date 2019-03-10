(ns com.ben-allred.letshang.common.views.components.toast
  (:require
    [com.ben-allred.letshang.common.services.store.actions :as actions]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.logging :as log]))


(defn ^:private toast-message [_toast-id _toast]
  (let [height (volatile! nil)]
    (fn [toast-id {:keys [body level state]}]
      (let [adding? (= state :init)
            removing? (= state :removing)]
        [:li.toast-message.message
         (cond-> {:ref   (fn [node]
                           (some->> node
                                    (.getBoundingClientRect)
                                    (.-height)
                                    (vreset! height)))
                  :class [({:success "is-success"
                            :error   "is-danger"
                            :warning "is-warning"
                            :info    "is-info"}
                            level)
                          (when adding? "adding")
                          (when removing? "removing")]}
           (and removing? @height) (update :style assoc :margin-top (str "-" @height "px")))
         [:div.message-body
          {:on-click #(store/dispatch (actions/remove-toast toast-id))
           :style    {:cursor :pointer}}
          [:div.body-text @body]]]))))

(defn toasts [toasts]
  [:div.toast-container
   [:ul.toast-messages
    (for [[toast-id toast] (take 2 (sort-by key toasts))]
      ^{:key toast-id}
      [toast-message toast-id toast])]])
