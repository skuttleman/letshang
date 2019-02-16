(ns com.ben-allred.letshang.ui.services.forms.standard
  (:require
    [cljs.core.async.impl.protocols :as async.protocols]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
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
                         :touched? false}))))

(defn ^:private trackable->model [trackable]
  (reduce-kv (fn [model path {:keys [current]}]
               (assoc-in model path current))
             {}
             trackable))

(defn ^:private check-for [working pred]
  (loop [[val :as working] (vals working)]
    (if (empty? working)
      false
      (or (pred val) (recur (rest working))))))

(defn ^:private swap* [{:keys [working] :as state} validator f f-args]
  (let [current (trackable->model working)
        next (apply f current f-args)]
    (->> next
         (diff-paths #{} [] current)
         (reduce (fn [working path]
                   (update working path assoc
                           :current (get-in next path)
                           :touched? true))
                 working)
         (assoc state :api-error nil :errors (validator next) :working))))

(defn ^:private init [validator model]
  {:working            (model->trackable model)
   :errors             (validator model)
   :status             :ready
   :persist-attempted? false})

(defn ^:private request* [request state validator]
  (-> request
      (ch/peek (comp (partial reset! state) (partial init validator))
               (comp (partial swap! state assoc :status :ready :api-error) :errors))))

(defn create [api validator]
  (let [state (r/atom nil)
        validator (or validator (constantly nil))]
    (-> api
        (forms/fetch)
        (request* state validator))
    (reify
      forms/IPersist
      (attempted? [_]
        (:persist-attempted? @state))
      (persist! [this]
        (swap! state assoc :persist-attempted? true)
        (cond
          (not (forms/ready? this))
          (ch/reject {:message "Form not ready"})

          (not (forms/valid? this))
          (ch/reject {:message "Form not valid" :errors (forms/errors this)})

          :else
          (let [model @this]
            (swap! state assoc :status :pending)
            (-> api
                (forms/save! model)
                (request* state validator)))))

      forms/ISync
      (ready? [this]
        (= :ready (forms/status this)))
      (status [_]
        (:status @state))

      forms/IChange
      (changed? [_]
        (check-for (:working @state) #(not= (:initial %) (:current %))))
      (changed? [_ path]
        (let [{:keys [current initial]} (get-in @state [:working path])]
          (not= current initial)))

      forms/ITrack
      (touch! [_ path]
        (swap! state assoc-in [:working path :touched?] true))
      (touched? [_]
        (check-for (:working @state) :touched?))
      (touched? [_ path]
        (get-in @state [:working path :touched?]))

      forms/IValidate
      (errors [this]
        (let [{:keys [errors api-error]} @state]
          (cond
            api-error api-error
            (forms/ready? this) errors)))
      (valid? [this]
        (empty? (forms/errors this)))

      IDeref
      (-deref [_]
        (when-let [working-model (:working @state)]
          (trackable->model working-model)))

      IReset
      (-reset! [_ model]
        (reset! state (init validator model)))

      ISwap
      (-swap! [_ f]
        (swap! state swap* validator f []))
      (-swap! [_ f a]
        (swap! state swap* validator f [a]))
      (-swap! [_ f a b]
        (swap! state swap* validator f [a b]))
      (-swap! [_ f a b xs]
        (swap! state swap* validator f (into [a b] xs))))))
