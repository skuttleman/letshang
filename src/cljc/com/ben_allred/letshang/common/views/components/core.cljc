(ns com.ben-allred.letshang.common.views.components.core
  (:require
    [clojure.set :as set]
    [com.ben-allred.letshang.common.utils.colls :as colls]))

(def ^:private level->class
  {:error "is-danger"})

(defn alert [level body]
  [:div.message
   {:class [(level->class level)]}
   [:div.message-body
    body]])

(def unicode
  (comp (partial conj [:span]) {:© #?(:clj "&#xa9;" :cljs "©")}))

(defn render [component & more-args]
  (-> component
      (or :<>)
      (colls/force-vector)
      (into more-args)))

(defn render-with-attrs [component attrs & more-args]
  (-> component
      (colls/force-vector)
      (update 1 merge attrs)
      (into more-args)))

(defn icon
  ([icon-class]
   (icon {} icon-class))
  ([attrs icon-class]
   [:i.fas (update attrs :class conj (str "fa-" (name icon-class)))]))

(defn tooltip [attrs & body]
  (-> (:text attrs)
      (if [:span.tooltip (-> attrs
                             (dissoc :position)
                             (update :class conj (str "is-tooltip-" (name (:position attrs :top))))
                             (set/rename-keys {:text :data-tooltip}))]
          [:span])
      (into body)))
