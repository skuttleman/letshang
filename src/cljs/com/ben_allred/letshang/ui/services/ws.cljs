(ns com.ben-allred.letshang.ui.services.ws
  (:require
    [clojure.core.async :as async]
    [clojure.core.match :refer-macros [match]]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.serde.transit :as transit]
    [com.ben-allred.letshang.common.utils.strings :as strings]
    [com.ben-allred.letshang.ui.services.navigation :as nav]
    [ws-client-cljc.core :as ws*]))

(defprotocol IWebSocket
  (-subscribe! [this topic])
  (-unsubscribe! [this topic]))

(defn ^:private send! [state body]
  (when-let [ws (:ws @state)]
    (async/put! ws body)))

(defn ^:private on-message [dispatch msg]
  (when-let [event (match msg
                     [:ws/message {:topic [:hangout _] :body [:messages/new body]}] [:ws/message.new body]
                     :else nil)]
    (dispatch event)))

(defonce ^:private socket
  (let [state (atom {:topics #{}})]
    (letfn [(connect! []
              (let [csrf (env/get :csrf-token)
                    uri (nav/path-for :api/events {:query-params {:x-csrf-token csrf}})]
                (when-let [ws (when (and csrf (.-WebSocket js/window))
                                (ws*/connect! (strings/format "ws%s://%s%s"
                                                              (if (= :https (env/get :protocol)) "s" "")
                                                              (env/get :host)
                                                              uri)
                                              {:in-buf-or-n  10
                                               :out-buf-or-n 100
                                               :in-xform     (comp (map transit/decode)
                                                                   (remove #{[:ws/ping]}))
                                               :out-xform    (map transit/encode)}))]
                  (swap! state assoc :ws ws)
                  (doseq [topic (:topics @state)]
                    (send! state [:subscriptions/subscribe topic]))
                  (async/go-loop []
                    (if-let [msg (async/<! ws)]
                      (do (on-message store/dispatch msg)
                          (recur))
                      (do (swap! state dissoc :ws)
                          (async/<! (async/timeout 1000))
                          (connect!)))))))]
      (connect!)
      (reify IWebSocket
        (-subscribe! [_ topic]
          (when-not (contains? (:topics @state) topic)
            (swap! state update :topics conj topic)
            (send! state [:subscriptions/subscribe topic])))
        (-unsubscribe! [_ topic]
          (when (contains? (:topics @state) topic)
            (swap! state update :topics disj topic)
            (send! state [:subscriptions/unsubscribe topic])))))))

(defn subscribe! [topic]
  (-subscribe! socket topic))

(defn unsubscribe! [topic]
  (-unsubscribe! socket topic))
