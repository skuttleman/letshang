(ns com.ben-allred.letshang.common.views.components.infinite
  (:refer-clojure :exclude [list])
  (:require
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.views.components.loading :as loading]))

(defn list [{:keys [fetch loading?]} items]
  (when (and (not loading?)
             (empty? items))
    (fetch))
  (fn [{:keys [buffer component fetch height key-fn loading? more?]
        :or   {height 300 buffer 200 key-fn str}} items]
    [:ul.layout--stack-between
     {:style     {:height (str height "px") :overflow-y :auto}
      :on-scroll (fn [e]
                   #?(:cljs
                      (when (and more? (not loading?))
                        (let [target (.-target e)
                              scroll-height (.-scrollHeight target)
                              scroll-top (.-scrollTop target)]
                          (when-not (pos? (- scroll-height scroll-top height buffer))
                            (fetch))))))}
     (for [item items]
       ^{:key (key-fn item)}
       [:li [component item]])
     (when loading?
       [:li [loading/spinner {:size :small}]])]))
