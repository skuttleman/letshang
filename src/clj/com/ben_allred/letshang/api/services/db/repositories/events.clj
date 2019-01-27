(ns com.ben-allred.letshang.api.services.db.repositories.events
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]))

(defn select-by* [clause]
  (-> entities/events
      (entities/select)
      (assoc :where clause)))
