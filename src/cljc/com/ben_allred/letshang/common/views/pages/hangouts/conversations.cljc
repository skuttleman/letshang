(ns com.ben-allred.letshang.common.views.pages.hangouts.conversations
  (:require
    [com.ben-allred.letshang.common.resources.hangouts.conversations :as res.conversations]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.store.actions.hangouts :as act.hangouts]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.views.components.fields :as fields]
    [com.ben-allred.letshang.common.views.components.form-view :as form-view]
    [com.ben-allred.letshang.common.views.components.infinite :as infinite]))

(defn ^:private message-item [message]
  [:div
   {:style {:background-color :white}}
   [:div (:body message)]
   [:div
    "by "
    (get-in message [:creator :handle])
    " at "
    (dates/format (:created-at message))]])

(defn ^:private message-form []
  (let [form (res.conversations/form)]
    (fn []
      [form-view/form
       {:inline?   true
        :form      form
        :save-text "Send"}
       [fields/textarea
        (-> {:placeholder "Type a message…"
             :auto-focus? true}
            (res.conversations/with-attrs form [:body])
            (update :errors #(when (forms/attempted? form) %)))]])))

(defn conversation [_attrs {{:keys [route-params]} :page :as state}]
  (let [[{:keys [status realized? length]} items] (:messages state)]
    [:div.layout--stack-between
     [message-form]
     [infinite/list
      {:component message-item
       :key-fn    :id
       :fetch     #(store/dispatch (act.hangouts/fetch-messages (:hangout-id route-params) length))
       :loading?  (= :requesting status)
       :more?     (not realized?)}
      items]]))
