(ns com.ben-allred.letshang.common.services.forms.dependent
  (:require
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.utils.chans :as ch])
  #?(:clj
     (:import
       (clojure.lang IAtom IDeref))))

(defn create [form isync]
  (reify
    forms/ISync
    (save! [_]
      (if (forms/ready? isync)
        (forms/save! form)
        (ch/reject)))
    (ready? [_]
      (and (forms/ready? isync)
           (forms/ready? form)))

    forms/IChange
    (changed? [_]
      (forms/changed? form))
    (changed? [_ path]
      (forms/changed? form path))

    forms/ITrack
    (attempted? [_]
      (forms/attempted? form))
    (visit! [_ path]
      (forms/visit! form path))
    (visited? [_ path]
      (forms/visited? form path))

    forms/IValidate
    (errors [_]
      (when (forms/ready? isync)
        (forms/errors form)))

    IDeref
    #?(:clj  (deref [_] @form)
       :cljs (-deref [_] @form))

    #?@(:clj  [IAtom
               (swap [_ f]
                 (swap! form f)
                 nil)
               (swap [_ f a]
                 (swap! form f a)
                 nil)
               (swap [_ f a b]
                 (swap! form f a b)
                 nil)
               (swap [_ f a b xs]
                 (apply swap! form f a b xs)
                 nil)]
        :cljs [ISwap
               (-swap! [_ f]
                 (swap! form f)
                 nil)
               (-swap! [_ f a]
                 (swap! form f a)
                 nil)
               (-swap! [_ f a b]
                 (swap! form f a b)
                 nil)
               (-swap! [_ f a b xs]
                 (apply swap! form f a b xs)
                 nil)])))
