(ns com.ben-allred.letshang.api.services.db.entities
  (:require
    [clojure.set :as set]))

(defn ^:private with-field-alias [fields alias]
  (let [alias' (name alias)]
    (map (fn [field]
           (let [field' (name field)]
             [(keyword (str alias' "." field'))
              (keyword alias' field')]))
         fields)))

(defn ^:private join* [query join entity alias on]
  (-> query
      (update join (fnil conj []) [(:table entity) alias] on)
      (update :select into (with-field-alias (:fields entity) alias))))

(defn with-alias [entity alias]
  (-> entity
      (update :select with-field-alias alias)
      (update :from (comp vector conj) alias)))

(defn insert-into [entity rows]
  {:insert-into (:table entity)
   :values      (map #(select-keys % (:fields entity)) rows)})


(defn modify [entity m]
  {:update (:table entity)
   :set    (select-keys m (:fields entity))})

(defn select [entity]
  (-> entity
      (set/rename-keys {:fields :select :table :from})
      (update :from vector)))

(defn left-join
  ([query entity on]
   (left-join query entity (:table entity) on))
  ([query entity alias on]
   (join* query :left-join entity alias on)))

(defn inner-join
  ([query entity on]
   (inner-join query entity (:table entity) on))
  ([query entity alias on]
   (join* query :join entity alias on)))

(def hangouts
  {:fields #{:id :name :created-by :created-at}
   :table  :hangouts})

(def known-associates
  {:fields #{:id :user-id :associate-id :created-by :created-at}
   :table  :known-associates})

(def invitees
  {:fields #{:id :hangout-id :user-id :match-type :created-by :created-at}
   :table  :invitees})

(def users
  {:fields #{:id :first-name :last-name :handle :email :mobile-number :created-at}
   :table  :users})
