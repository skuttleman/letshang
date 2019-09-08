(ns com.ben-allred.letshang.common.views.components.flip-move
  (:require
    #?(:cljs cljsjs.react-flip-move)
    [com.ben-allred.letshang.common.stubs.reagent :as r]))

(def flip-move (r/adapt-react-class #?(:cljs js/FlipMove)))
