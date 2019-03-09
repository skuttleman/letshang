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
