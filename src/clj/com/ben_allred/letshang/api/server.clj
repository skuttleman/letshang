(ns com.ben-allred.letshang.api.server
  (:gen-class)
  (:require
    [clojure.string :as string]
    [nrepl.server :as nrepl]
    [com.ben-allred.letshang.api.routes.core :as routes]
    [com.ben-allred.letshang.api.utils.respond :as respond]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.common.utils.numbers :as numbers]
    [com.ben-allred.letshang.common.utils.strings :as strings]
    [compojure.core :refer [ANY DELETE GET POST PUT context defroutes]]
    [compojure.handler :refer [site]]
    [compojure.response :refer [Renderable]]
    [immutant.web :as web]
    [ring.middleware.reload :refer [wrap-reload]])
  (:import
    (clojure.lang IPersistentVector)
    (java.net InetAddress)))

(extend-protocol Renderable
  IPersistentVector
  (render [this _]
    (respond/with this)))

(defn ^:private server-port [env key fallback]
  (let [port (str (or (get env key) (env/get key) fallback))]
    (numbers/parse-int! port)))

(defn ^:private run [port app]
  (web/run app {:port port :host "0.0.0.0"})
  (println "Server is listening on port" port))

(defn ^:private start! [port app]
  (run port app))

(defn -main [& {:as env}]
  (start! (server-port env :port 3000) #'routes/app))

(defn -dev [& {:as env}]
  (let [env (maps/map-keys (comp keyword string/lower-case strings/snake->kebab) env)
        port (server-port env :port 3000)
        nrepl-port (server-port env :nrepl-port 7000)
        base-url (format "http://%s:%d" (:canonicalHostName (bean (InetAddress/getLocalHost))) port)]
    (alter-var-root #'env/get merge env {:dev? true :base-url base-url :port port})
    (start! port #'routes/app-dev)
    (println "Server is running with #'wrap-reload")
    (nrepl/start-server :port nrepl-port)
    (println "REPL is listening on port" nrepl-port)))
