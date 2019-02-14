(ns com.ben-allred.letshang.api.utils.respond
  (:require
    [clojure.core.match :refer [match]]
    [com.ben-allred.letshang.common.services.http :as http]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn ^:private ->response [response status body headers]
  (cond-> response
    status (assoc :status (http/kw->status status status))
    body (assoc :body body)
    headers (assoc :headers headers)))

(defn with [[status body headers]]
  (->response {:status 200} status body headers))

(defn abort! [reason]
  (let [[status body headers] (match reason
                                (msg :guard string?) [nil {:message msg} nil]
                                [status (msg :guard string?)] [status {:message msg} nil]
                                [status (msg :guard string?) headers] [status {:message msg} headers]
                                :else reason)
        response (->response {:status 500} status body headers)]
    (throw (ex-info "abort!" {:response response}))))
