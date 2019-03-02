(ns com.ben-allred.letshang.common.services.transformers
  (:require
    [com.ben-allred.letshang.common.utils.logging :as log]))

(defn ^:private digits [s]
  (->> s
       (map str)
       (filter (partial re-matches #"\d"))
       (take 10)
       (apply str)))

(defn phone->view [s]
  (let [[_ open? close? space? hyphen?] (re-matches #"(\()?[^\)]*(\))?( )?[^-]*(-)?.*" (str s))
        [_ area prefix line] (->> s
                                  (digits)
                                  (re-matches #"(\d{0,3})(\d{0,3})(\d{0,4})"))
        area? (= 3 (count area))
        prefix? (= 3 (count prefix))]
    (cond-> ""
      (or open? (seq area)) (str "(")
      (seq area) (str area)
      (and area? (or close? space? hyphen? (seq prefix))) (str ")")
      (and area? (or space? hyphen? (seq prefix))) (str " ")
      (seq prefix) (str prefix)
      (and prefix? (or hyphen? (seq line))) (str "-")
      (seq line) (str line))))

(defn phone->model [s]
  (->> s
       (digits)
       (not-empty)))
