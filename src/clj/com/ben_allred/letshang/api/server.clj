(ns com.ben-allred.letshang.api.server
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as string]
    [clojure.tools.nrepl.server :as nrepl]
    [com.ben-allred.letshang.api.services.html :as html]
    [com.ben-allred.letshang.api.services.middleware :as middleware]
    [com.ben-allred.letshang.api.utils.respond :as respond]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.common.utils.numbers :as numbers]
    [compojure.core :refer [ANY DELETE GET POST PUT context defroutes]]
    [compojure.handler :refer [site]]
    [compojure.response :refer [Renderable]]
    [compojure.route :as route]
    [immutant.web :as web]
    [ring.middleware.reload :refer [wrap-reload]])
  (:import
    (clojure.lang IPersistentVector)))

(extend-protocol Renderable
  IPersistentVector
  (render [this _]
    (respond/with this)))

(defroutes ^:private base
  (context "/" []
    (route/resources "/")
    (GET "/health" [] [:http.status/ok {:a :ok}])
    (GET "/*" req [:http.status/ok
                   (-> req
                       (select-keys #{:uri :query-string})
                       (html/render))
                   {"content-type" "text/html"}])
    (ANY "/*" [] [:http.status/not-found])))

(def ^:private app
  (-> #'base
      (site)
      (middleware/abortable)
      (middleware/content-type)
      (middleware/log-response)))

(defn ^:private server-port [env key fallback]
  (let [port (str (or (get env key) (env/get key) fallback))]
    (numbers/parse-int! port)))

(defn ^:private run [env]
  (let [port (server-port env :port 3000)]
    (web/run #'app {:port port})
    (println "Server is listening on port" port)))

(defn -main [& {:as env}]
  (run env))

(defn -dev [& {:as env}]
  (alter-var-root #'env/get assoc :dev? true)
  (s/check-asserts true)
  (let [env (maps/map-keys (comp keyword
                                 string/lower-case
                                 #(string/replace % #"_" "-"))
                           env)
        nrepl-port (server-port env :nrepl-port 7000)]
    (run env)
    (println "Server is running with #'wrap-reload")
    (nrepl/start-server :port nrepl-port)
    (println "REPL is listening on port" nrepl-port)))
