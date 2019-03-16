(ns com.ben-allred.letshang.api.services.db.repositories.moments
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]))

(defn select-by [clause]
  (-> entities/moments
      (entities/select)
      (entities/with-alias :moments)
      (assoc :where clause)))

(defn insert [moment]
  (entities/insert-into entities/moments [moment]))

(defn id-clause
  ([clause moment-id]
    [:and clause (id-clause moment-id)])
  ([moment-id]
    [:= :moments.id moment-id]))

(defn hangout-ids-clause
  ([clause hangout-ids]
   [:and clause (hangout-ids-clause hangout-ids)])
  ([hangout-ids]
   [:in :moments.hangout-id hangout-ids]))

(defn moment-window-clause
  ([clause moment]
   [:and clause (moment-window-clause moment)])
  ([{:keys [hangout-id date moment-window]}]
    [:and
     [:= :moments.hangout-id hangout-id]
     [:= :moments.date date]
     [:= :moments.moment-window moment-window]]))
