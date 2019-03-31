(ns com.ben-allred.letshang.api.routes.auth
  (:require
    [com.ben-allred.letshang.api.services.db.models.users :as models.users]
    [com.ben-allred.letshang.api.services.handlers :refer [GET POST context]]
    [com.ben-allred.letshang.api.services.navigation :as nav]
    [com.ben-allred.letshang.api.utils.respond :as respond]
    [com.ben-allred.letshang.common.resources.sign-up :as sign-up.res]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.encoders.jwt :as jwt]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [compojure.core :refer [defroutes]]
    [ring.util.response :as resp]))

(def ^:private sign-up-spec
  {:data sign-up.res/validator})

(defn ^:private token->cookie [resp cookie value]
  (->> value
       (assoc {:path "/" :http-only true} :value)
       (merge cookie)
       (assoc-in resp [:cookies "auth-token"])))

(defn ^:private redirect
  ([cookie value]
   (redirect :ui/home cookie value))
  ([route cookie value]
   (-> (env/get :base-url)
       (str (nav/path-for route))
       (resp/redirect)
       (token->cookie cookie value))))

(defn ^:private logout []
  (redirect {:max-age 0} ""))

(defn ^:private login [user sign-up-user]
  (cond
    (seq user)
    (redirect nil (jwt/encode {:user user}))

    (and (seq sign-up-user)
         (-> sign-up-user
             (sign-up.res/validator)
             (select-keys (keys sign-up-user))
             (empty?)))
    (redirect nil (jwt/encode {:sign-up sign-up-user}))

    :else
    (logout)))

(defn ^:private check-conflicts! [db user]
  (if-let [conflict (models.users/select-conflicts db user)]
    (-> {:message "Unable to create user"}
        (cond->
          (= (:handle conflict) (:handle user))
          (assoc-in [:errors :data :handle] ["Screen name in use"])

          (= (:email conflict) (:email user))
          (assoc-in [:errors :data :email] ["Email in use"])

          (= (:mobile-number conflict) (:mobile-number user))
          (assoc-in [:errors :data :mobile-number] ["Phone number in use"]))
        (->> (conj [:http.status/bad-request]))
        (respond/abort!))
    user))

(defroutes routes
  (context "/auth" []
    (POST "/register"
          ^{:request-spec sign-up-spec}
          {{:keys [data]} :body :keys [db auth/sign-up]}
      (->> (merge sign-up (select-keys data #{:handle :first-name :last-name :mobile-number}))
           (check-conflicts! db)
           (models.users/create db)
           (hash-map :data)
           (conj [:http.status/created])))
    (GET "/login" {:keys [params]}
      (-> (env/get :base-url)
          (str (nav/path-for :auth/callback {:query-params (select-keys params #{:email})}))
          (resp/redirect)))
    (GET "/callback" {{:keys [email]} :params :keys [db]}
      (-> email
          (->> (models.users/find-by-email db))
          (some-> (select-keys #{:first-name :last-name :id :handle}))
          (login {:email email})))
    (GET "/logout" []
      (logout))))
