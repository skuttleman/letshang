(ns com.ben-allred.letshang.api.utils.respond
  (:require
    [clojure.core.match :refer [match]]
    [com.ben-allred.letshang.common.services.http :as http]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn ^:private ->response [default reason]
  (let [[status body headers] (cond-> reason
                                (= 500 (:status default))
                                (match
                                  (msg :guard string?) [nil {:message msg} nil]
                                  [status (msg :guard string?)] [status {:message msg} nil]
                                  [status (msg :guard string?) headers] [status {:message msg} headers]
                                  :else reason))]
    (cond-> default
      status (assoc :status (http/kw->status status status))
      body (assoc :body body)
      headers (assoc :headers headers))))

(defn with [reason]
  (->response {:status 200} reason))

(defn abort! [reason]
  (->> reason
       (->response {:status 500})
       (hash-map :response)
       (ex-info "abort!")
       (throw)))
