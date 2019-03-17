(ns com.ben-allred.letshang.api.services.db.repositories.hangouts
  (:require
    [clojure.set :as set]
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defmethod repos/->api ::model
  [_ hangout]
  (-> hangout
      (set/rename-keys {:others-invite     :others-invite?
                        :when-suggestions  :when-suggestions?
                        :where-suggestions :where-suggestions?})))

(defmethod repos/->db ::model
  [_ hangout]
  (-> hangout
      (set/rename-keys {:others-invite?     :others-invite
                        :when-suggestions?  :when-suggestions
                        :where-suggestions? :where-suggestions})))

(defn select-by [clause]
  (-> entities/hangouts
      (entities/select)
      (entities/with-alias :hangouts)
      (assoc :where clause)))

(defn insert [hangout]
  (entities/insert-into entities/hangouts [hangout]))

(defn modify [hangout clause]
  (-> entities/hangouts
      (entities/modify hangout)
      (assoc :where clause)))

(defn id-clause
  ([clause hangout-id]
   [:and clause (id-clause hangout-id)])
  ([hangout-id]
   [:= :hangouts.id hangout-id]))

(defn creator-clause
  ([clause user-id]
   [:and clause (creator-clause user-id)])
  ([user-id]
   [:= :hangouts.created-by user-id]))

(defn has-hangout-clause
  ([clause user-id]
   [:and clause (has-hangout-clause user-id)])
  ([user-id]
   [:or
    [:= :hangouts.created-by user-id]
    [:exists {:select [:id]
              :from   [:invitations]
              :where  [:and
                       [:= :invitations.hangout-id :hangouts.id]
                       [:= :invitations.user-id user-id]]}]]))
