(ns com.ben-allred.letshang.integration.utils.helpers
  (:require
    [com.ben-allred.letshang.integration.utils.http :as test.http]
    [com.ben-allred.letshang.api.services.db.migrations :as migrations]
    [com.ben-allred.letshang.common.services.http :as http]))

(defn login [email]
  (-> (test.http/request* http/get nil (str "/auth/login?email=" email) nil false)
      (get-in [3 :cookies "auth-token" :value])))

(defn seed! [seed-file]
  (migrations/seed! (str "db/test/" seed-file)))
