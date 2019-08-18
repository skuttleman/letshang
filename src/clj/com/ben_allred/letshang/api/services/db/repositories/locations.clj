(ns com.ben-allred.letshang.api.services.db.repositories.locations
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [honeysql.core :as sql]))

(defn select-by [clause]
  (-> entities/locations
      (entities/select)
      (entities/with-alias :locations)
      (assoc :where clause)))

(defn insert [location]
  (entities/insert-into entities/locations [location]))

(defn modify [location clause]
  (-> entities/locations
      (entities/modify location)
      (assoc :where clause)))

(defn id-clause
  ([clause location-id]
   [:and clause (id-clause location-id)])
  ([location-id]
   [:= :locations.id location-id]))

(defn hangout-id-clause
  ([clause hangout-id]
   [:and clause (hangout-id-clause hangout-id)])
  ([hangout-id]
   [:= :locations.hangout-id hangout-id]))

(defn hangout-ids-clause
  ([clause hangout-ids]
   [:and clause (hangout-ids-clause hangout-ids)])
  ([hangout-ids]
   [:in :locations.hangout-id hangout-ids]))

(defn location-hangout-name-clause
  ([clause location]
   [:and clause (location-hangout-name-clause location)])
  ([{:keys [hangout-id name]}]
   [:and
    [:= :locations.hangout-id hangout-id]
    [:= (sql/call :lower :locations.name) (sql/call :lower name)]]))
