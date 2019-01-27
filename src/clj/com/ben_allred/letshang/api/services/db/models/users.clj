(ns com.ben-allred.letshang.api.services.db.models.users
  (:require
    [com.ben-allred.letshang.api.services.db.models.shared :as models]
    [com.ben-allred.letshang.api.services.db.repositories.core :as repos]
    [com.ben-allred.letshang.api.services.db.repositories.users :as repo.users]
    [com.ben-allred.letshang.common.utils.colls :as colls]))

(def ^:private model
  (reify models/Model
    (->api [_ user]
      (dissoc user :created-at))
    (->db [_ user]
      (dissoc user :created-at))))

(defn select-by-email [email]
  (-> [:= :email email]
      (repo.users/select-by*)
      (models/select model)
      (repos/exec!)))

(defn find-by-email [email]
  (-> email
      (select-by-email)
      (colls/only!)))
