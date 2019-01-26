(ns com.ben-allred.letshang.ui.utils.dom
  (:require
    [clojure.set :as set]))

(def ^:private listeners (atom {}))

(def ^:private key->code
  {:key-codes/esc 27
   :key-codes/enter 13})

(def ^:private code->key
  (set/map-invert key->code))

(defn stop-propagation [event]
  (when (.-stopPropagation event)
    (.stopPropagation event)))

(defn prevent-default [event]
  (when (.-preventDefault event)
    (.preventDefault event)))

(defn target-value [event]
  (some-> event
          (.-target)
          (.-value)))

(defn query-one [selector]
  (.querySelector js/document selector))

(defn click [node]
  (.click node))

(defn focus [node]
  (.focus node)
  (when (.-setSelectionRange node)
    (let [length (-> node (.-value) (.-length))]
      (.setSelectionRange node length length))))

(defn event->key [e]
  (-> e
      (.-keyCode)
      (code->key)))

(defn add-listener [node event cb]
  (let [key (gensym)
        listener [node event (.addEventListener node event cb)]]
    (swap! listeners assoc key listener)
    key))

(defn remove-listener [key]
  (when-let [[node event id] (get @listeners key)]
    (swap! listeners dissoc key)
    (.removeEventListener node event id)))
