(ns com.ben-allred.letshang.api.services.db.repositories.users
  (:require
    [com.ben-allred.letshang.api.services.db.entities :as entities]))

(defn select-by [clause]
  (-> entities/users
      (entities/select)
      (entities/with-alias :users)
      (assoc :where clause)))

(defn select-known-associates [user-id]
  (let [known-associates {:from [(:table entities/known-associates)]}]
    (-> entities/users
        (entities/select)
        (entities/with-alias :associates)
        (assoc :where [:or
                       [:in :associates.id (assoc known-associates
                                                  :select #{:user-id}
                                                  :where [:= :associate-id user-id])]
                       [:in :associates.id (assoc known-associates
                                                  :select #{:associate-id}
                                                  :where [:= :user-id user-id])]]))))

(defn insert [user]
  (entities/insert-into entities/users [user]))

(defn email-clause
  ([clause email]
   [:and clause (email-clause email)])
  ([email]
   [:= :users.email email]))

(defn conflict-clause
  ([clause user]
   [:and clause (conflict-clause user)])
  ([{:keys [email handle mobile-number]}]
   [:or
    [:= :handle handle]
    [:= :email email]
    [:= :mobile-number mobile-number]]))
