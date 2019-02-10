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

(defn render [component & [more-attrs? :as more-args]]
  (let [[component & [attrs? :as args?]] (colls/force-vector (or component :<>))
        [attrs & args] (if (map? attrs?)
                         args?
                         (cons nil args?))
        [more-attrs & more-args] (if (map? more-attrs?)
                                   more-args
                                   (cons nil more-args))]
    (-> [component]
        (cond-> (or attrs more-attrs) (conj (merge attrs more-attrs)))
        (into (concat args more-args)))))
