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
