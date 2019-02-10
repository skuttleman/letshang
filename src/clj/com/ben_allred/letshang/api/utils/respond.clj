(ns com.ben-allred.letshang.api.utils.respond
  (:require
    [clojure.core.match :refer [match]]
    [com.ben-allred.letshang.common.services.http :as http]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn with [[status body headers]]
  (cond-> {:status 200}
    status (assoc :status (http/kw->status status status))
    body (assoc :body body)
    headers (assoc :headers headers)))

(defn abort! [reason]
  (let [[status body headers] (match reason
                                (msg :guard string?) [nil {:message msg} nil]
                                [status (msg :guard string?)] [status {:message msg} nil]
                                [status (msg :guard string?) headers] [status {:message msg} headers]
                                :else reason)
        response (cond-> {:status 500}
                   status (assoc :status (http/kw->status status status))
                   body (assoc :body body)
                   headers (assoc :headers headers))]
    (throw (ex-info "abort!" {:response response}))))
