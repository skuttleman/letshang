(ns com.ben-allred.letshang.api.routes.auth
  (:require
    [com.ben-allred.letshang.api.services.db.models.users :as models.users]
    [com.ben-allred.letshang.api.services.navigation :as nav]
    [com.ben-allred.letshang.api.utils.respond :as respond]
    [com.ben-allred.letshang.common.resources.sign-up :as sign-up.res]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.serde.jwt :as jwt]
    [com.ben-allred.letshang.common.utils.serde.query-params :as qp]
    [ring.util.response :as resp])
  (:import (java.net URI)))

(def ^:private sign-up-spec
  {:data sign-up.res/validator})

(def ^:private transform-spec
  {:redirect-uri qp/decode-param})

(defn ^:private token->cookie [resp cookie value]
  (->> value
       (assoc {:path "/" :http-only true} :value)
       (merge cookie)
       (assoc-in resp [:cookies "auth-token"])))

(defn ^:private redirect
  ([route value]
   (redirect route value nil))
  ([route value cookie]
   (let [path (cond
                (keyword? route) (nav/path-for route)
                (or (nil? route) (.isAbsolute (URI. route))) (nav/path-for :ui/home)
                :else route)]
     (-> (env/get :base-url)
         (str path)
         (resp/redirect)
         (token->cookie cookie value)))))

(defn ^:private logout* []
  (redirect :ui/home "" {:max-age 0}))

(defn ^:private login* [user sign-up-user redirect-uri]
  (cond
    (seq user)
    (redirect redirect-uri (jwt/encode {:user user}))

    (and (seq sign-up-user)
         (-> sign-up-user
             (sign-up.res/validator)
             (select-keys (keys sign-up-user))
             (empty?)))
    (redirect redirect-uri (jwt/encode {:sign-up sign-up-user}))

    :else
    (logout*)))

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

(defn register [{{:keys [data]} :body :keys [db auth/sign-up]}]
  (->> (merge sign-up (select-keys data #{:handle :first-name :last-name :mobile-number}))
       (check-conflicts! db)
       (models.users/create db)
       (hash-map :data)
       (conj [:http.status/created])))

(defn login [{:keys [params]}]
  (-> (env/get :base-url)
      (str (nav/path-for :auth/callback {:query-params (select-keys params #{:email :redirect-uri})}))
      (resp/redirect)))

(defn logout [_]
  (logout*))

(defn callback [{{:keys [email redirect-uri]} :params :keys [db]}]
  (-> email
      (->> (models.users/find-by-email db))
      (some-> (select-keys #{:first-name :last-name :id :handle}))
      (login* {:email email} redirect-uri)))

(def routes
  {:auth/callback ^{:transformer transform-spec}
                  {:get #'callback}
   :auth/login    ^{:transformer transform-spec}
                  {:get #'login}
   :auth/logout   ^{:transformer transform-spec}
                  {:get #'logout}
   :auth/register ^{:transformer transform-spec :request-spec sign-up-spec}
                  {:post #'register}})
