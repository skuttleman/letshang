(ns com.ben-allred.letshang.api.services.db.repositories.users
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]))

(defn select-by* [clause]
  (-> entities/users
      (entities/select)
      (assoc :where clause)))
