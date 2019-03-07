(ns com.ben-allred.letshang.common.services.store.ui-reducers
  (:require
    [com.ben-allred.collaj.reducers :as collaj.reducers]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.common.utils.logging :as log #?@(:cljs [:include-macros true])]
    [com.ben-allred.letshang.common.utils.maps :as maps #?@(:cljs [:include-macros true])]))

(defn ^:private moment->respond [{:keys [responses] :as moment}]
  (assoc moment :response-counts (frequencies (map :response responses))))

(defn ^:private add-or-replace-moment [moment response]
  (-> moment
      (cond->
        (= (:moment-id response) (:id moment))
        (update :responses (partial colls/assoc-by (juxt :moment-id :user-id) response)))
      (moment->respond)))

(defn ^:private resource [namespace]
  (let [[request success error] (->> [:request :success :error]
                                     (map (partial keywords/namespaced namespace)))]
    (fn
      ([] [:init])
      ([state [type response]]
       (condp = type
         request [:requesting]
         success [:success (:data response)]
         error [:error response]
         state)))))

(defn ^:private modify-hangout
  ([] [:init])
  ([state [type response]]
   (case type
     :hangout/success [:success (update (:data response)
                                        :moments
                                        (partial map moment->respond))]
     :moment/success (update-in state
                                [1 :moments]
                                colls/supdate map add-or-replace-moment
                                (:data response))
     :suggestions.when/success (update-in state
                                          [1 :moments]
                                          conj
                                          (moment->respond (:data response)))
     state)))

(def ^:private associates (resource :associates))
(def ^:private hangouts (resource :hangouts))
(def ^:private hangout (collaj.reducers/comp modify-hangout (resource :hangout)))

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
                                       page
                                       toasts)))
