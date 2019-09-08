(ns com.ben-allred.letshang.common.views.components.auto-scroll
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn scroller [component ideas size ms & _args]
  (let [idea-items (r/atom [0 (cycle ideas)])
        mounted? (volatile! true)]
    #?(:cljs
       (async/go-loop []
         (swap! idea-items (fn [[length items]]
                             (if (< length size)
                               [(inc length) items]
                               [length (rest items)])))
         (async/<! (async/timeout ms))
         (when @mounted?
           (recur))))
    (r/create-class
      {:component-will-unmount
       (fn [_]
         (vreset! mounted? false))
       :reagent-render
       (fn [_component _ideas _size _ms & args]
         (let [[length items] @idea-items]
           (into [component (sequence (comp (take length)
                                            (map-indexed (fn [idx item]
                                                           [[(when (= idx (dec length)) "squish")] item])))
                                      items)]
                 args)))})))
