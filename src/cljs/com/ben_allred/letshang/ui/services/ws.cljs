(ns com.ben-allred.letshang.ui.services.ws
  (:require
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.encoders.transit :as transit]
    [com.ben-allred.letshang.common.utils.strings :as strings]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.ui.services.navigation :as nav]))

(declare subscribe! unsubscribe!)

(defn ^:private on-message [dispatch]
  (fn [event]
    (let [message (transit/decode (.-data event))]
      (when (not= [:ws/ping] message)
        (dispatch [:ws/message (log/spy message)])))))

(defn ^:private send! [ws body]
  (when ws
    (.send ws (transit/encode body))))

(defn connect! [dispatch]
  (let [path (nav/path-for :api/events)
        ws (js/WebSocket. (strings/format "ws%s://%s%s"
                                          (if (= :https (env/get :protocol)) "s" "")
                                          (env/get :host)
                                          path))]
    (when ws
      (doto ws
        (.addEventListener "message" (on-message dispatch)))

      (defn subscribe! [topic]
        (send! ws [:subscriptions/subscribe topic]))

      (defn unsubscribe! [topic]
        (send! ws [:subscriptions/unsubscribe topic])))))
