(ns com.ben-allred.letshang.api.services.db.repositories.moments
  (:require
    [clojure.set :as set]
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.preparations :as prep]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.common.utils.maps :as maps]))

(defmethod repos/->api ::model
  [_ moment]
  (-> moment
      (set/rename-keys {:moment-window :window})
      (maps/update-maybe :window keyword)))

(defmethod repos/->db ::model
  [_ moment]
  (-> moment
      (set/rename-keys {:window :moment-window})))

(defmethod repos/->sql-value [:moments :moment-window]
  [_ _ value]
  (prep/moments-window value))

(defmethod repos/->sql-value [:moments :date]
  [_ _ value]
  (prep/date value))

(defmethod repos/->sql-value [:moments :moment-time]
  [_ _ value]
  (prep/time value))

(defn select-by [clause]
  (-> entities/moments
      (entities/select)
      (entities/with-alias :moments)
      (assoc :where clause)))

(defn insert [moment]
  (entities/insert-into entities/moments [moment]))

(defn modify [moment clause]
  (-> entities/moments
      (entities/modify moment)
      (assoc :where clause)))

(defn id-clause
  ([clause moment-id]
   [:and clause (id-clause moment-id)])
  ([moment-id]
   [:= :moments.id moment-id]))

(defn hangout-id-clause
  ([clause hangout-id]
   [:and clause (hangout-id-clause hangout-id)])
  ([hangout-id]
   [:= :moments.hangout-id hangout-id]))

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
