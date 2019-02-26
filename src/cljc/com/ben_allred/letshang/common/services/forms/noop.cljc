(ns com.ben-allred.letshang.common.services.forms.noop
  (:require
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [clojure.core.async :as async])
  #?(:clj
     (:import
       (clojure.lang IAtom IDeref))))

(defn create [model]
  (reify
    forms/IPersist
    (attempted? [_] false)
    (persist! [_] (async/go [:success model]))

    forms/ISync
    (ready? [_])
    (status [_])

    forms/IChange
    (changed? [_] false)
    (changed? [_ _] false)

    forms/ITrack
    (visit! [_ _])
    (visited? [_ _] false)


    forms/IValidate
    (errors [_] nil)
    (valid? [_] true)

    #?@(:clj  [IAtom
               (reset [_ _])
               (swap [_ _])
               (swap [_ _ _])
               (swap [_ _ _ _])
               (swap [_ _ _ _ _])
               IDeref
               (deref [_]
                 model)]
        :cljs [IReset
               (-reset! [_ _])
               ISwap
               (-swap! [_ _])
               (-swap! [_ _ _])
               (-swap! [_ _ _ _])
               (-swap! [_ _ _ _ _])
               IDeref
               (-deref [_]
                 model)])))
