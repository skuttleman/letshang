(ns com.ben-allred.letshang.common.stubs.reagent
  (:refer-clojure :exclude [atom])
  #?(:cljs
     (:require
       [reagent.core :as reagent])))

(def atom #?(:clj clojure.core/atom :cljs reagent/atom))

(def create-class #?(:clj :reagent-render :cljs reagent/create-class))

(def argv #?(:clj (constantly nil) :cljs reagent/argv))
