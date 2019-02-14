(ns com.ben-allred.letshang.api.server
  (:gen-class)
  (:require
    [clojure.string :as string]
    [clojure.tools.nrepl.server :as nrepl]
    [com.ben-allred.letshang.api.routes.core :as routes]
    [com.ben-allred.letshang.api.utils.respond :as respond]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.common.utils.numbers :as numbers]
    [compojure.core :refer [ANY DELETE GET POST PUT context defroutes]]
    [compojure.handler :refer [site]]
    [compojure.response :refer [Renderable]]
    [immutant.web :as web]
    [ring.middleware.reload :refer [wrap-reload]])
  (:import
    (clojure.lang IPersistentVector)))

(extend-protocol Renderable
  IPersistentVector
  (render [this _]
    (respond/with this)))

(defn ^:private server-port [env key fallback]
  (let [port (str (or (get env key) (env/get key) fallback))]
    (numbers/parse-int! port)))

(defn ^:private run [env app]
  (let [port (server-port env :port 3000)]
    (web/run app {:port port :host "0.0.0.0"})
    (println "Server is listening on port" port)))

(defn -main [& {:as env}]
  (run env #'routes/app))

(defn -dev [& {:as env}]
  (alter-var-root #'env/get assoc :dev? true)
  (let [env (maps/map-keys (comp keyword
                                 string/lower-case
                                 #(string/replace % #"_" "-"))
                           env)
        nrepl-port (server-port env :nrepl-port 7000)]
    (run env #'routes/app-dev)
    (println "Server is running with #'wrap-reload")
    (nrepl/start-server :port nrepl-port)
    (println "REPL is listening on port" nrepl-port)))
