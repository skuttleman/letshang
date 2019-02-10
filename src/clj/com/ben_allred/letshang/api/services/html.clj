(ns com.ben-allred.letshang.api.services.html
  (:require
    [com.ben-allred.collaj.core :as collaj]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.services.navigation :as nav*]
    [com.ben-allred.letshang.common.services.ui-reducers :as ui-reducers]
    [com.ben-allred.letshang.common.templates.core :as templates]
    [com.ben-allred.letshang.common.utils.encoders.transit :as transit]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.common.views.core :as views]
    [hiccup.core :as hiccup]))

(defn ^:private template [content user]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name :viewport :content "width=device-width, initial-scale=1"}]
    [:title "Let's Hang"]
    [:link {:rel       "stylesheet"
            :href      "https://use.fontawesome.com/releases/v5.6.3/css/all.css"
            :integrity "sha384-UHRtZLI+pbxtHCWp1t77Bi1L4ZtiqrqD80Kn4Z8NTSRyMA2Fd33n5dQ8lWUE00s/"
            :crossorigin "anonymous"
            :type      "text/css"}]
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
                                                    :auth/user user)
                                  (transit/encode)
                                  (pr-str)))]
    [:script {:type "text/javascript" :src "/js/compiled/app.js"}]
    [:script
     {:type "text/javascript"}
     "com.ben_allred.letshang.ui.app.mount_BANG_();"]]])

(defn hydrate [page user]
  (let [{:keys [get-state]} (collaj/create-store ui-reducers/root)]
    (-> (get-state)
        (assoc :page page :auth/user user)
        (views/app)
        (templates/render)
        (template user)
        (hiccup/html)
        (str "<!DOCTYPE html>"))))

(defn render [{:keys [uri query-string auth/user]}]
  (-> uri
      (cond->
        query-string (str "?" query-string))
      (->> (nav*/match-route nav*/app-routes))
      (hydrate user)))
