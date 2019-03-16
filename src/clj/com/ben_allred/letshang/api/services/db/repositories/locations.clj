(ns com.ben-allred.letshang.api.services.db.repositories.locations
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]))

(defn select-by [clause]
  (-> entities/locations
      (entities/select)
      (entities/with-alias :locations)
      (assoc :where clause)))

(defn insert [location]
  (entities/insert-into entities/locations [location]))

(defn id-clause
  ([clause location-id]
   [:and clause (id-clause location-id)])
  ([location-id]
   [:= :locations.id location-id]))

(defn hangout-ids-clause
  ([clause hangout-ids]
   [:and clause (hangout-ids-clause hangout-ids)])
  ([hangout-ids]
   [:in :locations.hangout-id hangout-ids]))
