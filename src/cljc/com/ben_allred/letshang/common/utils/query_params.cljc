(ns com.ben-allred.letshang.common.utils.query-params
  (:require
    [clojure.string :as string]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]))

(defn ^:private namify [[k v]]
  [(str (keywords/safe-name k)) (str (keywords/safe-name v))])

(defn ^:private parsify [[k v]]
  [(keyword k) (or v true)])

(defn parse [s]
  (into {}
        (comp (map #(string/split % #"="))
              (filter (comp seq first))
              (map parsify))
        (string/split (str s) #"&")))

(defn stringify [qp]
  (->> qp
       (map (comp (partial string/join "=") namify))
       (string/join "&")))
