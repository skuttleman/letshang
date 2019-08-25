(ns com.ben-allred.letshang.common.services.forms.noop
  (:require
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [clojure.core.async :as async])
  #?(:clj
     (:import
       (clojure.lang IAtom IDeref))))

(defn create [model]
  (reify
    forms/ISync
    (save! [_] (async/go [:success model]))

    forms/IBlock
    (ready? [_] false)

    forms/IChange
    (changed? [_] false)
    (changed? [_ _] false)

    forms/ITrack
    (attempted? [_] false)
    (visit! [_ _])
    (visited? [_ _] false)

    forms/IValidate
    (errors [_] nil)

    #?@(:clj  [IAtom
               (swap [_ _])
               (swap [_ _ _])
               (swap [_ _ _ _])
               (swap [_ _ _ _ _])
               IDeref
               (deref [_]
                 model)]
        :cljs [ISwap
               (-swap! [_ _])
               (-swap! [_ _ _])
               (-swap! [_ _ _ _])
               (-swap! [_ _ _ _ _])
               IDeref
               (-deref [_]
                 model)])))
