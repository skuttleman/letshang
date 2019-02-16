(ns com.ben-allred.letshang.common.views.components.core
  (:require
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
