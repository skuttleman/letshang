(ns com.ben-allred.letshang.api.services.db.repositories.sessions
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]
    [crypto.random :as rand]))

(defn select-by [clause]
  (-> entities/sessions
      (entities/select)
      (entities/with-alias :sessions)
      (assoc :where clause)))

(defn upsert [user-id]
  (let [session-id (rand/base64 60)]
    (entities/upsert entities/sessions [{:id session-id :user-id user-id}] [:user-id] [:user-id])))

(defn id-clause
  ([clause id]
   [:and clause (id-clause id)])
  ([id]
   [:= :sessions.id id]))

(defn user-id-clause
  ([clause user-id]
   [:and clause (user-id-clause user-id)])
  ([user-id]
   [:= :sessions.user-id user-id]))
