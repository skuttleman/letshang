(ns com.ben-allred.letshang.api.services.db.repositories.users
  (:require
    [clojure.string :as string]
    [com.ben-allred.letshang.api.services.db.entities :as entities]))

(defn select-by [clause]
  (-> entities/users
      (entities/select)
      (entities/with-alias :users)
      (assoc :where clause)))

(defn select-known-associates [user-id]
  (-> entities/users
      (entities/select)
      (entities/with-alias :associates)
      (assoc :where [:and
                     [:not= :associates.id user-id]
                     [:or
                      [:in
                       :associates.id
                       {:select [:hangouts.created-by]
                        :from   [:hangouts]
                        :join   [:invitations [:and
                                               [:= :invitations.hangout-id :hangouts.id]
                                               [:= :invitations.user-id user-id]]]}]
                      [:in
                       :associates.id
                       {:select    [:invitations.user-id]
                        :from      [:hangouts]
                        :left-join [:invitations [:= :invitations.hangout-id :hangouts.id]]
                        :where     [:or
                                    [:= :hangouts.created-by user-id]
                                    [:exists {:select [:*]
                                              :from   [[:invitations :sub]]
                                              :where  [:and
                                                       [:= :sub.hangout-id :hangouts.id]
                                                       [:= :sub.user-id user-id]]}]]}]]])))

(defn insert [user]
  (entities/insert-into entities/users [user]))

(defn email-clause
  ([clause email]
   [:and clause (email-clause email)])
  ([email]
   [:= :users.email (string/lower-case (string/trim email))]))

(defn conflict-clause
  ([clause user]
   [:and clause (conflict-clause user)])
  ([{:keys [email handle mobile-number]}]
   [:or
    [:= :handle handle]
    [:= :email email]
    [:= :mobile-number mobile-number]]))
