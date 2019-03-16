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

(defn id-clause
  ([clause invitation-id]
   [:and clause (id-clause invitation-id)])
  ([invitation-id]
   [:= :invitations.id invitation-id]))

(defn user-clause
  ([clause user-id]
   [:and clause (user-clause user-id)])
  ([user-id]
   [:= :invitations.user-id user-id]))

(defn hangout-id-clause
  ([clause hangout-id]
   [:and clause (hangout-id-clause hangout-id)])
  ([hangout-id]
   [:= :invitations.hangout-id hangout-id]))

(defn hangout-ids-clause
  ([clause hangout-ids]
   [:and clause (hangout-ids-clause hangout-ids)])
  ([hangout-ids]
   [:in :invitations.hangout-id hangout-ids]))

(defn has-hangout-clause
  ([clause user-id]
   [:and clause (has-hangout-clause user-id)])
  ([user-id]
   [:or
    [:= :invitations.user-id user-id]
    [:= :hangouts.created-by user-id]]))
