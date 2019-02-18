(ns com.ben-allred.letshang.api.services.db.repositories.invitations
  (:require [com.ben-allred.letshang.api.services.db.entities :as entities]))

(defn select-by [clause]
  (-> entities/invitations
      (entities/select)
      (entities/with-alias :invitations)
      (assoc :where clause)))

(defn modify [invitation clause]
  (-> entities/invitations
      (entities/modify invitation)
      (assoc :where clause)))
