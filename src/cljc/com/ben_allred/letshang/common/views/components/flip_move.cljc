(ns com.ben-allred.letshang.common.views.components.flip-move
  (:require
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    #?(:cljs cljsjs.react-flip-move)))

(def flip-move (r/adapt-react-class #?(:cljs js/FlipMove)))
