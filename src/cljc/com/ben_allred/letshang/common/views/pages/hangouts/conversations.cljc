(ns com.ben-allred.letshang.common.views.pages.hangouts.conversations
  (:require
    [com.ben-allred.letshang.common.resources.hangouts.conversations :as res.conversations]
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.views.components.fields :as fields]
    [com.ben-allred.letshang.common.views.components.form-view :as form-view]))

(defn ^:private message-item [message]
  [:li
   {:style {:background-color :white}}
   [:div (:body message)]
   [:div
    "by "
    (get-in message [:creator :handle])
    " at "
    (dates/format (:created-at message))]])

(defn ^:private messages* [{:keys [messages]}]
  [:div.layout--inset
   [:ul.layout--stack-between
    (for [message messages]
      ^{:key (:id message)}
      [message-item message])]])

(defn ^:private message-form [hangout-id]
  (let [form (res.conversations/form hangout-id)]
    (fn [_hangout-id]
      [form-view/form
       {:inline?   true
        :form      form
        :save-text "Send"}
       [fields/textarea
        (-> {:placeholder "Type a message…"
             :auto-focus? true}
            (res.conversations/with-attrs form [:body]))]])))

(defn conversation [_attrs {{hangout-id :id} :hangout :as state}]
  [:div.layout--stack-between
   [message-form hangout-id]
   [messages* state]])
