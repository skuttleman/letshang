(ns com.ben-allred.letshang.ui.services.ws
  (:require
    [cljs.core.match :refer [match]]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.encoders.transit :as transit]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.strings :as strings]
    [com.ben-allred.letshang.ui.services.navigation :as nav]
    [cljs.core.async :as async]))

(defonce ^:private socket (atom {:topics #{}}))

(defn ^:private send! [body]
  (when-let [ws (:ws @socket)]
    (when (= (.-readyState ws) (.-OPEN js/WebSocket))
      (.send ws (transit/encode body)))))

(defn ^:private on-message [dispatch]
  (fn [event]
    (when-let [event (match (transit/decode (.-data event))
                       [:ws/ping] nil
                       [:ws/message {:topic [:hangout _] :body [:messages/new body]}] [:ws/message.new body]
                       :else nil)]
      (dispatch event))))

(defn subscribe! [topic]
  (swap! socket update :topics conj topic)
  (send! [:subscriptions/subscribe topic]))

(defn unsubscribe! [topic]
  (swap! socket update :topics disj topic)
  (send! [:subscriptions/unsubscribe topic]))

(defn connect! [dispatch]
  (let [csrf (env/get :csrf-token)
        uri (nav/path-for :api/events {:query-params {:x-csrf-token csrf}})]
    (when-let [ws (when (and csrf (.-WebSocket js/window))
                    (js/WebSocket. (strings/format "ws%s://%s%s"
                                                   (if (= :https (env/get :protocol)) "s" "")
                                                   (env/get :host)
                                                   uri)))]
      (doto ws
        (.addEventListener "message" (on-message dispatch))
        (.addEventListener "close" #(do
                                      (swap! socket dissoc :ws)
                                      (async/go
                                        (async/<! (async/timeout 1000))
                                        (connect! dispatch))))
        (.addEventListener "open" #(do
                                     (swap! socket assoc :ws ws)
                                     (doseq [topic (:topics @socket)]
                                       (subscribe! topic))))))))
