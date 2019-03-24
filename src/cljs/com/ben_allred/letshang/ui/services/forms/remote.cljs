(ns com.ben-allred.letshang.ui.services.forms.remote
  (:require
    [com.ben-allred.letshang.common.services.forms.core :as forms]))

(defn create [f]
  (reify
    forms/ISync
    (ready? [_]
      (#{:success :error} (first (f))))

    forms/IValidate
    (errors [_]
      (let [[status value] (f)]
        (when (= :error status)
          value)))

    IDeref
    (-deref [_]
      (let [[status value] (f)]
        (when (= :success status)
          value)))))

(defn combine [& remotes]
  (reify
    forms/ISync
    (ready? [_]
      (every? forms/ready? remotes))

    forms/IValidate
    (errors [_]
      (map forms/errors remotes))

    IDeref
    (-deref [_]
      (map deref remotes))))
