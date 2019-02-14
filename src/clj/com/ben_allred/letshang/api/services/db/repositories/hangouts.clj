(ns com.ben-allred.letshang.api.services.db.repositories.hangouts
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn select-by* [clause]
  (-> entities/hangouts
      (entities/select)
      (entities/with-alias :hangouts)
      (assoc :where clause)))

(defn insert [hangout]
  (entities/insert-into entities/hangouts [hangout]))
