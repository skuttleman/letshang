(ns com.ben-allred.letshang.common.services.store.ui-reducers
  (:require
    [com.ben-allred.collaj.reducers :as collaj.reducers]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log #?@(:cljs [:include-macros true])]
    [com.ben-allred.letshang.common.utils.maps :as maps #?@(:cljs [:include-macros true])]))

(defn ^:private count-respondable [{:keys [responses] :as respondable}]
  (assoc respondable :response-counts (frequencies (map :response responses))))

(defn ^:private add-or-replace-response [respondable id-fn response]
  (-> respondable
      (cond->
        (= (id-fn response) (:id respondable))
        (update :responses (partial colls/assoc-by (juxt id-fn :user-id) response)))
      (count-respondable)))

(defn ^:private add-or-replace-respondable [respondable response]
  (cond-> respondable
    (= (:id respondable) (:id response)) (-> (assoc :responses (:responses response))
                                             (count-respondable))))

(defn ^:private add-or-replace-respondables [respondables response]
  (colls/assoc-by :id (count-respondable response) (map #(add-or-replace-respondable % response) respondables)))

(defn ^:private resource [namespace invalidate?]
  (let [[request success error invalidate!] (map (partial keywords/namespaced namespace)
                                                 [:request :success :error :invalidate!])]
    (fn
      ([] [:init])
      ([state [type data]]
       (condp = type
         request [:requesting]
         success [:success (:data data)]
         error [:error data]
         invalidate! (if (invalidate? (second state) data)
                       [:init]
                       state)
         state)))))

(def ^:private associates (resource :associates (constantly false)))
(def ^:private hangouts (resource :hangouts (constantly true)))
(def ^:private hangout (resource :hangout #(= (:id %1) %2)))

(defn ^:private invitations
  ([] [:init])
  ([state [type response]]
   (case type
     :invitations/request [:requesting]
     :invitations/success [:success (:data response)]
     :invitations/error [:error response]
     :invitations/invalidate! [:init]
     :response.invitation/success [:success (colls/assoc-by :id (:data response) (second state))]
     :suggestions.who/success [:success (reduce add-or-replace-respondables (second state) (:data response))]
     state)))

(defn ^:private locations
  ([] [:init])
  ([state [type response]]
   (case type
     :locations/request [:requesting]
     :locations/success [:success (map count-respondable (:data response))]
     :locations/error [:error response]
     :locations/invalidate! [:init]
     :location/success [:success (add-or-replace-respondables (second state) (:data response))]

     :response.location/success [:success (colls/supdate (second state) map add-or-replace-response :location-id (:data response))]
     :suggestions.where/success [:success (add-or-replace-respondables (second state) (:data response))]
     state)))

(def ^:private messages-init [{:status :init :length 0 :realized? false} []])

(defn ^:private messages
  ([] messages-init)
  ([[meta data :as state] [type response]]
   (case type
     :messages/request [(assoc meta :status :requesting) data]
     :messages/success (let [length (count (:data response))]
                         [(-> meta
                              (assoc :status :success)
                              (update :length + length)
                              (cond-> (zero? length) (assoc :realized? true)))
                          (concat data (:data response))])
     :messages/error [(assoc meta :status :error :error response) data]
     :messages/invalidate! messages-init
     :ws/message.new [(update meta :length inc) (cons response data)]
     state)))

(defn ^:private moments
  ([] [:init])
  ([state [type response]]
   (case type
     :moments/request [:requesting (when (not= [:init] state) state)]
     :moments/success [:success (map count-respondable (:data response))]
     :moments/error [:error response]
     :moment/success [:success (add-or-replace-respondables (second state) (:data response))]
     :moments/invalidate! [:init]
     :response.moment/success [:success (colls/supdate (second state) map add-or-replace-response :moment-id (:data response))]
     :suggestions.when/success [:success (add-or-replace-respondables (second state) (:data response))]
     state)))

(defn ^:private page
  ([] nil)
  ([state [type page]]
   (case type
     :router/navigate page
     state)))

(defn ^:private toasts
  ([] {})
  ([state [type {:keys [id level body]}]]
   (case type
     :toast/add (assoc state id {:state :init :level level :body body})
     :toast/show (maps/update-maybe state id assoc :state :showing)
     :toast/hide (maps/update-maybe state id assoc :state :removing)
     :toast/remove (dissoc state id)
     state)))

(def root
  (collaj.reducers/combine (maps/->map associates
                                       :auth/user (constantly (env/get :auth/user))
                                       :auth/sign-up (constantly (env/get :auth/sign-up))
                                       hangout
                                       hangouts
                                       invitations
                                       locations
                                       messages
                                       moments
                                       page
                                       toasts)))
