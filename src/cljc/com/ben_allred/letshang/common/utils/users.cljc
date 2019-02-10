(ns com.ben-allred.letshang.common.utils.users
  (:require
    [clojure.string :as string]
    [com.ben-allred.letshang.common.utils.strings :as strings]))

(defn full-name [user]
  (->> [(:first-name user)
        (:last-name user)]
       (filter some?)
       (string/join " ")
       (strings/trim-to-nil)))
