(ns com.ben-allred.letshang.api.routes.auth
  (:require
    [com.ben-allred.letshang.api.services.db.models.users :as models.users]
    [com.ben-allred.letshang.api.services.handlers :refer [GET context]]
    [com.ben-allred.letshang.api.services.navigation :as nav]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.encoders.jwt :as jwt]
    [compojure.core :refer [defroutes]]
    [ring.util.response :as resp]))


(defn ^:private token->cookie [resp cookie value]
  (->> value
       (assoc {:path (nav/path-for :ui/home) :http-only true} :value)
       (merge cookie)
       (assoc-in resp [:cookies "auth-token"])))

(defn ^:private redirect [cookie value]
  (-> (env/get :base-url)
      (str (nav/path-for :ui/home))
      (resp/redirect)
      (token->cookie cookie value)))

(defn ^:private logout []
  (redirect {:max-age 0} ""))

(defn ^:private login [user]
  (if (seq user)
    (redirect nil (jwt/encode user))
    (logout)))

(defroutes routes
  (context "/auth" []
    (GET "/login" {:keys [params]}
      (-> (env/get :base-url)
          (str (nav/path-for :auth/callback {:query-params (select-keys params #{:email})}))
          (resp/redirect)))
    (GET "/callback" {:keys [params]}
      (-> params
          (:email)
          (models.users/find-by-email)
          (login)))
    (GET "/logout" []
      (logout))))
