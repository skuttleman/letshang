(ns com.ben-allred.letshang.common.views.components
  (:require
    [#?(:clj clojure.core.async :cljs cljs.core.async) :as async]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.stubs.store :as store]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]))

(defn spinner [{:keys [size]}]
  [:div.loader
   {:class [(keywords/safe-name size)]}])

(def unicode
  (comp (partial conj [:span]) {:© #?(:clj "&#xa9;" :cljs "©")}))

(defn auto-scroller [component ideas size ms]
  (let [idea-items (r/atom [0 (cycle ideas)])
        mounted? (atom true)]
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
         (reset! mounted? false))
       :reagent-render
       (fn [_component _ideas _size _ms]
         (let [[length items] @idea-items]
           (->> items
                (take length)
                (map-indexed (fn [idx item]
                               [[(when (= idx (dec length)) "squish")] item]))
                (conj [component]))))})))

(def ^:private level->class
  {:error "is-danger"})

(defn alert [level tree]
  [:div.message
   {:class [(level->class level)]}
   [:div.message-body
    tree]])

(defn with-status [{:keys [action]}]
  (let [finished? (r/atom false)]
    (async/go
      (async/<! (store/dispatch action))
      (reset! finished? true))
    (fn [{:keys [tree data-fn state]}]
      (let [[status data] (data-fn state)]
        (cond
          (and (= status :success) @finished?)
          (conj (colls/force-sequential tree) data)

          (and (= status :error) @finished?)
          [alert :error (:message data "An unknown error occurred")]

          :else
          [:div.center-content [spinner {:size :large}]])))))
