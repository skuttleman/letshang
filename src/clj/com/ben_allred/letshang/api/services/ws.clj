(ns com.ben-allred.letshang.api.services.ws
  (:require
    [clojure.core.async :as async]
    [clojure.core.match :refer [match]]
    [com.ben-allred.letshang.api.services.db.models.hangouts :as models.hangouts]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.common.utils.encoders.transit :as transit]
    [com.ben-allred.letshang.common.utils.fns :refer [=>]]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.uuids :as uuids]
    [immutant.web.async :as web.async]))

(defn has-hangout? [hangout-id user-id]
  (repos/transact #(models.hangouts/find-for-user % hangout-id user-id)))

(defonce ^:private subscribers (ref {}))

(defn ^:private cleanup [m k id ch-id]
  (if-let [chs (not-empty (disj (get-in m [k id]) ch-id))]
    (assoc-in m [k id] chs)
    (update m k dissoc id)))

(defn ^:private handle-subscribe [ch-id topic {user-id :id}]
  (when (match topic
          [:hangout hangout-id] (has-hangout? hangout-id user-id)
          :else true)
    (dosync
      (alter subscribers (=> (update-in [:topics topic] (fnil conj #{}) ch-id)
                             (update-in [:channels ch-id :topics] conj topic))))))

(defn ^:private handle-unsubscribe [ch-id topic]
  (dosync
    (alter subscribers (=> (update-in [:channels ch-id :topics] disj topic)
                           (cleanup :topics topic ch-id)))))

(defn ^:private handle-open [ch-id {user-id :id}]
  (fn [ch]
    (dosync
      (alter subscribers (=> (update-in [:users user-id] (fnil conj #{}) ch-id)
                             (assoc-in [:channels ch-id] {:user-id user-id
                                                          :topics  #{}
                                                          :ch      ch}))))))

(defn ^:private handle-close [ch-id {user-id :id}]
  (fn [ch data]
    (dosync
      (alter subscribers (fn [subs]
                           (let [topics (get-in subs [:channels ch-id :topics])]
                             (-> subs
                                 (update :channels dissoc ch-id)
                                 (cleanup :users user-id ch-id)
                                 (as-> $ (reduce #(cleanup %1 :topics %2 ch-id) $ topics)))))))))

(defn ^:private handle-message [ch-id user]
  (fn [ch body]
    (match (transit/decode body)
      [:subscriptions/subscribe topic] (handle-subscribe ch-id topic user)
      [:subscriptions/unsubscribe topic] (handle-unsubscribe ch-id topic)
      else (log/debug "Unknown message" else))))

(defn connect [{:keys [auth/user] :as req}]
  (let [ch-id (uuids/random)]
    (web.async/as-channel req {:on-open    (handle-open ch-id user)
                               :on-message (handle-message ch-id user)
                               :on-close   (handle-close ch-id user)})))

(defn ^:private send! [{:keys [ch]} body]
  (when ch
    (web.async/send! ch (transit/encode body))))

(defn ^:private publish-dispatch [target _ _] target)

(defmulti publish #'publish-dispatch)

(defn ^:private publish* [ch-ids body]
  (doseq [ch-id ch-ids
          :let [ws (get-in @subscribers [:channels ch-id])]]
    (send! ws body)))

(defmethod publish :topic
  [_ topic body]
  (publish* (get-in @subscribers [:topics topic]) [:ws/message {:topic topic :body body}]))

(defmethod publish :user
  [_ user-id body]
  (publish* (get-in @subscribers [:users user-id]) [:ws/message {:topic [:user user-id] :body body}]))

(defmethod publish :broadcast
  [_ _ body]
  (publish* (keys (:channels @subscribers)) [:ws/broadcast {:body body}]))

(defmethod publish :default
  [_ _ _]
  nil)

(defn ^:private pinger! [ch]
  (when ch
    (async/close! ch))
  (async/go-loop []
    (async/<! (async/timeout 30000))
    (doseq [[_ ws] (:channels @subscribers)]
      (send! ws [:ws/ping]))
    (recur)))

(defonce ^:private ping! (pinger! nil))
