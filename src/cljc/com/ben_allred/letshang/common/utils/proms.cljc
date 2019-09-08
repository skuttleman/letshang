(ns com.ben-allred.letshang.common.utils.proms
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.vow.core :as v]))

(defn ^:private promisified? [val]
  (and (vector? val) (#{:success :error} (first val))))

(defn from-reaction [reaction]
  (let [ch (async/chan)
        result @reaction
        prom (v/ch->prom ch (comp #{:success} first))
        k (gensym)]
    (if (promisified? result)
      (async/put! ch result)
      (add-watch reaction k (fn [_ _ _ result]
                              (when (promisified? result)
                                (remove-watch reaction k)
                                (async/put! ch result)))))
    (v/then prom second second)))

(defn from-ch [ch]
  (-> ch
      (v/ch->prom (comp #{:success} first))
      (v/then second second)))
