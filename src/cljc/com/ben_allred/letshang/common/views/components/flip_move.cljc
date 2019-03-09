(ns com.ben-allred.letshang.common.views.components.flip-move
  (:require
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    #?(:cljs cljsjs.react-flip-move)))

(def flip-move #?(:clj (constantly nil) :cljs (r/adapt-react-class js/FlipMove)))
