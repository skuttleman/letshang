(ns com.ben-allred.letshang.api.server
  (:gen-class)
  (:require
    [clojure.string :as string]
    [nrepl.server :as nrepl]
    [com.ben-allred.letshang.api.routes.core :as routes]
    [com.ben-allred.letshang.common.services.env :as env]
    [com.ben-allred.letshang.common.utils.maps :as maps]
    [com.ben-allred.letshang.common.utils.numbers :as numbers]
    [com.ben-allred.letshang.common.utils.strings :as strings]
    [immutant.web :as web]
    [ring.middleware.reload :refer [wrap-reload]])
  (:import
    (java.net InetAddress)))

(defn ^:private server-port [env key fallback]
  (let [port (str (or (get env key) (env/get key) fallback))]
    (numbers/parse-int! port)))

(defn ^:private run [port app]
  (let [server (web/run app {:port port :host "0.0.0.0"})]
    (println "Server is listening on port" port)
    server))

(defn ^:private start! [port app]
  (run port app))

(defn -main [& {:as env}]
  (let [env (maps/map-keys (comp keyword string/lower-case strings/snake->kebab) env)]
    (start! (server-port env :port 3000) #'routes/app)))

(defn -dev [& {:as env}]
  (let [env (maps/map-keys (comp keyword string/lower-case strings/snake->kebab) env)
        port (server-port env :port 3000)
        nrepl-port (server-port env :nrepl-port 7000)
        base-url (format "http://%s:%d" (.getCanonicalHostName (InetAddress/getLocalHost)) port)
        server (start! port #'routes/app-dev)]
    (alter-var-root #'env/get merge env {:dev? true :base-url base-url :port port})
    (println "Server is running with #'wrap-reload at" base-url)
    (nrepl/start-server :port nrepl-port)
    (println "REPL is listening on port" nrepl-port)
    server))
