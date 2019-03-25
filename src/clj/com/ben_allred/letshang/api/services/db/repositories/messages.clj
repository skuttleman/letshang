(ns com.ben-allred.letshang.api.services.db.repositories.messages
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]))

(defn select-by [clause]
  (-> entities/messages
      (entities/select)
      (entities/with-alias :messages)
      (assoc :where clause)))

(defn insert [message]
  (entities/insert-into entities/messages [message]))

(defn hangout-id-clause
  ([clause hangout-id]
   [:and clause (hangout-id-clause hangout-id)])
  ([hangout-id]
   [:= :messages.hangout-id hangout-id]))

(defn id-clause
  ([clause message-id]
   [:and clause (id-clause message-id)])
  ([message-id]
   [:= :messages.id message-id]))
