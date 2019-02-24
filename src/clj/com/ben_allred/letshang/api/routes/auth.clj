(ns com.ben-allred.letshang.api.routes.auth
  (:require
    [com.ben-allred.letshang.api.services.db.models.users :as models.users]
    [com.ben-allred.letshang.api.services.handlers :refer [GET POST context]]
    [com.ben-allred.letshang.api.services.navigation :as nav]
    [com.ben-allred.letshang.api.utils.respond :as respond]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.encoders.jwt :as jwt]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [compojure.core :refer [defroutes]]
    [ring.util.response :as resp]))


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

(defn ^:private login [sign-up-user user]
  (cond
    (seq user)
    (redirect nil (jwt/encode {:user user}))

    (seq sign-up-user)
    (redirect nil (jwt/encode {:sign-up sign-up-user}))

    :else
    (logout)))

(defroutes routes
  (context "/auth" []
    (POST "/register" {{:keys [data]} :body :keys [db]}
      (respond/with
        (if-let [user (models.users/create db data)]
          [:http.status/created {:data user}]
          [:http.status/bad-request {:message "Could not create user"}])))
    (GET "/login" {:keys [params]}
      (-> (env/get :base-url)
          (str (nav/path-for :auth/callback {:query-params (select-keys params #{:email})}))
          (resp/redirect)))
    (GET "/callback" {{:keys [email]} :params :keys [db]}
      (->> email
           (models.users/find-by-email db)
           (login {:email email})))
    (GET "/logout" []
      (logout))))
