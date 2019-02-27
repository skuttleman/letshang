(ns com.ben-allred.letshang.common.services.validators)

(def email-re #"^[a-z\-\+_0-9\.]+@[a-z\-\+_0-9]+\.[a-z\-\+_0-9\.]+$")
(def phone-re #"^\d{10}$")

(defn ^:private matches? [pattern]
  (fn [s]
    (boolean (and (string? s) (re-matches pattern s)))))

(def ^{:arglists '([s])} email?
  (matches? email-re))

(def ^{:arglists '([s])} phone-number?
  #?(:clj  (matches? phone-re)
     :cljs (some-fn (matches? #"\(\d{3}\) \d{3}-\d{4}") (matches? phone-re))))
