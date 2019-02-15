(ns com.ben-allred.letshang.api.services.db.migrations
  (:require
    [clojure.string :as string]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.utils.numbers :as numbers]
    [com.ben-allred.letshang.common.utils.strings :as strings]
    [ragtime.jdbc :as rag-db]
    [ragtime.repl :as rag]))

(defn ^:private date-str []
  (-> (dates/now)
      (dates/format :fs)))

(defn load-config []
  {:datastore  (rag-db/sql-database repos/db-cfg)
   :migrations (rag-db/load-resources "migrations")})

(defn migrate! []
  (rag/migrate (load-config)))

(defn rollback!
  ([]
   (rollback! 1))
  ([n]
   (let [cfg (load-config)]
     (loop [rollbacks n]
       (when (pos? rollbacks)
         (rag/rollback cfg)
         (recur (dec rollbacks)))))))

(defn redo! []
  (rollback!)
  (migrate!))

(defn speedbump! []
  (migrate!)
  (redo!))

(defn create! [name]
  (let [migration-name (format "%s_%s"
                               (date-str)
                               (-> name
                                   (strings/kebab->snake)
                                   (string/lower-case)))]
    (spit (format "resources/migrations/%s.up.sql" migration-name) "\n")
    (spit (format "resources/migrations/%s.down.sql" migration-name) "\n")
    (println "created migration: " migration-name)))

(defn ^:export run [command & [arg :as args]]
  (case command
    "migrate" (migrate!)
    "rollback" (rollback! (numbers/parse-int! (or arg "1")))
    "speedbump" (speedbump!)
    "redo" (redo!)
    "create" (create! (string/join "_" args))
    (throw (ex-info (str "unknown command: " command) {:command command :args args}))))
