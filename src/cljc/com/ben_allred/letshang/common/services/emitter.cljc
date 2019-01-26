(ns com.ben-allred.letshang.common.services.emitter
  (:require
    [clojure.core.async :as async #?@(:cljs [:include-macros true])]
    [clojure.core.async.impl.protocols :as impl]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defprotocol IEmitter
  (on [this env chan] [this env event chan])
  (publish [this env event data]))

(defn new []
  (let [listeners (atom {})]
    (reify IEmitter
      (on [this env chan]
        (on this env ::all chan))
      (on [this env event chan]
        (swap! listeners update-in [env event] conj chan)
        this)
      (publish [this env event data]
        (when-let [chans (-> listeners
                             (swap! update-in [env event] (partial remove impl/closed?))
                             (env)
                             (select-keys [::all event])
                             (#(mapcat val %))
                             (seq))]
          (->> chans
               (map #(async/put! % [event data]))
               (dorun)))
        this))))
