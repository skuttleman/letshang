(ns com.ben-allred.letshang.api.services.db.repositories.invitations
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.api.services.db.preparations :as prep]))

(defmethod repos/->api ::model
  [_ invitation]
  (-> invitation
      (maps/update-maybe :response keyword)
      (maps/update-maybe :match-type keyword)))

(defmethod repos/->sql-value [:invitations :match-type]
  [_ _ value]
  (prep/invitations-match-type value))

(defmethod repos/->sql-value [:invitations :response]
  [_ _ value]
  (prep/user-response value))

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
