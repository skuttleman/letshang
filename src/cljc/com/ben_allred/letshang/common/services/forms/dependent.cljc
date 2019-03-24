(ns com.ben-allred.letshang.common.services.forms.dependent
  (:require
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.utils.chans :as ch])
  #?(:clj
     (:import
       (clojure.lang IAtom IDeref))))

(defn create [form isync]
  (reify
    forms/IPersist
    (attempted? [_]
      (forms/attempted? form))
    (persist! [_]
      (if (forms/ready? isync)
        (forms/persist! form)
        (ch/reject)))

    forms/ISync
    (ready? [_]
      (and (forms/ready? isync)
           (forms/ready? form)))

    forms/IChange
    (changed? [_]
      (forms/changed? form))
    (changed? [_ path]
      (forms/changed? form path))

    forms/ITrack
    (visit! [_ path]
      (forms/visit! form path))
    (visited? [_ path]
      (forms/visited? form path))

    forms/IValidate
    (errors [_]
      (when (forms/ready? isync)
        (forms/errors form)))
    (valid? [_]
      (and (forms/ready? isync)
           (forms/valid? form)))

    IDeref
    #?(:clj  (deref [_] @form)
       :cljs (-deref [_] @form))

    #?@(:clj  [IAtom
               (reset [_ model] (reset! form model))
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
        :cljs [IReset
               (-reset! [_ model]
                  (reset! form model)
                  nil)

               ISwap
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
