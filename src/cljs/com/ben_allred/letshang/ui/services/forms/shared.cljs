(ns com.ben-allred.letshang.ui.services.forms.shared
  (:require
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]))

(defn ^:private diff-paths [paths path old-model new-model]
  (reduce-kv (fn [paths k v]
               (let [path (conj path k)]
                 (cond
                   (map? v) (diff-paths paths path (get old-model k) v)
                   (not= (get old-model k) v) (conj paths path)
                   :else paths)))
             paths
             new-model))

(defn ^:private nest [paths path model]
  (reduce-kv (fn [paths k v]
               (let [path (conj path k)]
                 (if (map? v)
                   (nest paths path v)
                   (assoc paths path v))))
             paths
             model))

(defn ^:private model->trackable [model]
  (->> model
       (nest {} [])
       (maps/map-vals (fn [value]
                        {:current  value
                         :initial  value
                         :visited? false}))))

(defn ^:private add-api-errors [{:keys [model] :as state} errors]
  (->> errors
       (nest {} [])
       (reduce (fn [m [path errors]]
                 (update m path assoc (get-in model path) errors))
               {})
       (assoc state :status :ready :api-errors)))

(defn trackable->model [trackable]
  (reduce-kv (fn [model path {:keys [current]}]
               (assoc-in model path current))
             {}
             trackable))

(defn check-for [working pred]
  (loop [[val :as working] (vals working)]
    (if (empty? working)
      false
      (or (pred val) (recur (rest working))))))

(defn swap* [{:keys [working] :as state} validator f f-args]
  (let [current (trackable->model working)
        next (apply f current f-args)]
    (->> next
         (diff-paths #{} [] current)
         (reduce (fn [working path]
                   (update working path assoc :current (get-in next path)))
                 working)
         (assoc state
                :model next
                :errors (validator next)
                :working))))

(defn init [validator model]
  {:errors             (validator model)
   :model              model
   :persist-attempted? false
   :status             :ready
   :working            (model->trackable model)})

(defn request* [request state validator]
  (-> request
      (ch/peek (comp (partial reset! state) (partial init validator))
               (comp (partial swap! state add-api-errors) :errors))))
