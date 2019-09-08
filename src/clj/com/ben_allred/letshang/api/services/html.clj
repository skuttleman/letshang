(ns com.ben-allred.letshang.api.services.html
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.collaj.core :as collaj]
    [com.ben-allred.letshang.api.services.db.models.hangouts :as models.hangouts]
    [com.ben-allred.letshang.api.services.db.models.invitations :as models.invitations]
    [com.ben-allred.letshang.api.services.db.models.locations :as models.locations]
    [com.ben-allred.letshang.api.services.db.models.messages :as models.messages]
    [com.ben-allred.letshang.api.services.db.models.moments :as models.moments]
    [com.ben-allred.letshang.api.services.db.models.sessions :as models.sessions]
    [com.ben-allred.letshang.api.services.db.models.users :as models.users]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.services.navigation :as nav*]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.services.store.ui-reducers :as ui-reducers]
    [com.ben-allred.letshang.common.templates.core :as templates]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.common.utils.serde.transit :as transit]
    [com.ben-allred.letshang.common.views.core :as views]
    [hiccup.core :as hiccup]))

(defn ^:private template [content state user sign-up csrf-ch]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name :viewport :content "width=device-width, initial-scale=1"}]
    [:title "Let's Hang"]
    [:link {:rel "shortcut icon" :href "/favicon.ico" :type "image/x-icon"}]
    [:link {:rel "icon" :href "/favicon.ico" :type "image/x-icon"}]
    [:link {:rel         "stylesheet"
            :href        "https://use.fontawesome.com/releases/v5.6.3/css/all.css"
            :integrity   "sha384-UHRtZLI+pbxtHCWp1t77Bi1L4ZtiqrqD80Kn4Z8NTSRyMA2Fd33n5dQ8lWUE00s/"
            :crossorigin "anonymous"
            :type        "text/css"}]
    [:link {:rel  "stylesheet"
            :href "https://cdnjs.cloudflare.com/ajax/libs/bulma/0.7.1/css/bulma.min.css"
            :type "text/css"}]
    [:link {:rel  "stylesheet"
            :href "https://cdn.jsdelivr.net/npm/bulma-tooltip@2.0.2/dist/css/bulma-tooltip.min.css"
            :type "text/css"}]
    [:link {:rel  "stylesheet"
            :href "/css/main.css"
            :type "text/css"}]]
   [:body
    [:div#app content]
    [:script
     {:type "text/javascript"}
     (format "window.ENV=%s;" (-> {}
                                  (maps/assoc-maybe :dev? (env/get :dev?)
                                                    :auth/user user
                                                    :auth/sign-up sign-up
                                                    :csrf-token (async/<!! csrf-ch)
                                                    :initial-state state)
                                  (transit/encode)
                                  (pr-str)))]
    [:script {:type "text/javascript" :src "/js/compiled/app.js"}]
    [:script
     {:type "text/javascript"}
     "com.ben_allred.letshang.ui.app.mount_BANG_();"]]])

(defn ^:private hydrate* [{:keys [dispatch get-state]} page {user-id :id}]
  (repos/transact
    (fn [db]
      (case (:handler page)
        :ui/hangouts (dispatch [:hangouts/success {:data (models.hangouts/select-for-user db user-id)}])
        :ui/hangout (let [{:keys [hangout-id section]} (:route-params page)]
                      (dispatch [:hangout/success {:data (models.hangouts/find-for-user db hangout-id user-id)}])
                      (case section
                        :conversation (->> {:data (models.messages/select-for-hangout db
                                                                                      hangout-id
                                                                                      user-id
                                                                                      {:limit  models.messages/LIMIT
                                                                                       :offset 0})}
                                           (conj [:messages/success])
                                           (dispatch))
                        :invitations (do
                                       (->> {:data (models.invitations/select-for-hangout db hangout-id user-id)}
                                            (conj [:invitations/success])
                                            (dispatch))
                                       (->> {:data (models.users/find-known-associates db user-id)}
                                            (conj [:associates/success])
                                            (dispatch)))
                        :locations (->> {:data (models.locations/select-for-hangout db hangout-id user-id)}
                                        (conj [:locations/success])
                                        (dispatch))

                        :moments (->> {:data (models.moments/select-for-hangout db hangout-id user-id)}
                                      (conj [:moments/success])
                                      (dispatch))))
        :ui/hangouts.new (->> {:data (models.users/find-known-associates db user-id)}
                              (conj [:associates/success])
                              (dispatch))
        nil)
      (get-state))))

(defn hydrate [page user sign-up]
  (let [csrf-ch (async/go
                  (when user
                    (:id (repos/transact #(models.sessions/upsert % (:id user))))))
        state (-> (hydrate* (collaj/create-store ui-reducers/root) page user)
                  (assoc :page page
                         :auth/user user
                         :auth/sign-up sign-up))]
    (binding [store/get-state (constantly state)]
      (-> state
          (views/app)
          (templates/render)
          (template state user sign-up csrf-ch)
          (hiccup/html)
          (->> (str "<!DOCTYPE html>"))))))

(defn render [{:keys [auth/sign-up auth/user query-string uri]}]
  (-> uri
      (cond->
        query-string (str "?" query-string))
      (->> (nav*/match-route nav*/app-routes))
      (hydrate user sign-up)))
