(ns com.ben-allred.letshang.common.stubs.reagent
  (:refer-clojure :exclude [atom])
  #?(:cljs
     (:require
       [reagent.core :as reagent]
       [reagent.ratom :as ratom]))
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(def atom #?(:clj clojure.core/atom :cljs reagent/atom))

(def create-class #?(:clj :reagent-render :cljs reagent/create-class))

(def argv #?(:clj (constantly nil) :cljs reagent/argv))

(def adapt-react-class #?(:clj (constantly nil) :cljs reagent/adapt-react-class))

(def make-reaction #?(:clj (constantly (reify IDeref (deref [_] nil))) :cljs ratom/make-reaction))