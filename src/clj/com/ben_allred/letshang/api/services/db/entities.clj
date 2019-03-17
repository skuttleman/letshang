(ns com.ben-allred.letshang.api.services.db.entities
  (:require
    [clojure.set :as set]))

(defn ^:private with-field-alias [fields alias overrides]
  (let [alias' (name alias)]
    (map (fn [field]
           (let [field' (name field)]
             [(keyword (str alias' "." field'))
              (get overrides field (keyword alias' field'))]))
         fields)))

(defn ^:private join* [query join entity alias on overrides]
  (-> query
      (update join (fnil conj []) [(:table entity) alias] on)
      (update :select into (with-field-alias (:fields entity) alias overrides))))

(defn with-alias
  ([entity alias]
   (with-alias entity alias nil))
  ([entity alias overrides]
   (-> entity
       (update :select with-field-alias alias overrides)
       (update :from (comp vector conj) alias))))

(defn insert-into [entity rows]
  {:insert-into (:table entity)
   :values      rows
   :returning   [:*]})

(defn upsert [entity rows conflict keys]
  (-> entity
      (insert-into rows)
      (assoc :on-conflict conflict :do-update-set keys)))

(defn on-conflict-nothing [query conflict]
  (assoc query :on-conflict conflict :do-nothing []))

(defn limit [query amt]
  (assoc query :limit amt))

(defn modify [entity m]
  {:update (:table entity)
   :set    m})

(defn select [entity]
  (-> entity
      (set/rename-keys {:fields :select :table :from})
      (update :from vector)))

(defn left-join
  ([query entity on]
   (left-join query entity (:table entity) on))
  ([query entity alias on]
   (left-join query entity alias on nil))
  ([query entity alias on overrides]
   (join* query :left-join entity alias on overrides)))

(defn inner-join
  ([query entity on]
   (inner-join query entity (:table entity) on))
  ([query entity alias on]
   (inner-join query entity alias on #{}))
  ([query entity alias on overrides]
   (join* query :join entity alias on overrides)))

(def hangouts
  {:fields #{:created-at :created-by :id :name :others-invite :when-suggestions :where-suggestions}
   :table :hangouts})

(def invitations
  {:fields #{:created-at :created-by :hangout-id :id :match-type :response :user-id}
   :table  :invitations})

(def locations
  {:fields #{:created-at :created-by :hangout-id :id :name}
   :table  :locations})

(def location-responses
  {:fields #{:location-id :response :user-id}
   :table :location-responses})

(def moments
  {:fields #{:created-at :created-by :date :hangout-id :id :moment-window}
   :table  :moments})

(def moment-responses
  {:fields #{:moment-id :response :user-id}
   :table :moment-responses})

(def users
  {:fields #{:created-at :email :first-name :handle :id :last-name :mobile-number}
   :table  :users})
