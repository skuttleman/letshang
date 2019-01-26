(ns com.ben-allred.letshang.api.services.db.entities
  (:require [clojure.set :as set]))

(defn ^:private with-field-alias [fields alias]
  (let [alias' (name alias)]
    (map (fn [field]
           (let [field' (name field)]
             [(keyword (str alias' "." field'))
              (keyword alias' field')]))
         fields)))

(defn with-alias [entity alias]
  (-> entity
      (update :select with-field-alias alias)
      (update :from (comp vector conj) alias)))

(defn select [entity]
  (-> entity
      (set/rename-keys {:fields :select :table :from})
      (update :from vector)))

(def events
  {:fields #{:id :name :created-by :created-at}
   :table  :events})

(def users
  {:fields #{:id :first-name :last-name :handle :email :mobile-number :created-at}
   :table  :users})
