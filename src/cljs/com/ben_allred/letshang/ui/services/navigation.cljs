(ns com.ben-allred.letshang.ui.services.navigation
  (:require
    [com.ben-allred.letshang.common.services.navigation :as nav*]
    [com.ben-allred.letshang.ui.services.store.core :as store]
    [pushy.core :as pushy]))

(defn match-route [path]
  (nav*/match-route nav*/ui-routes path))

(defn path-for
  ([page]
   (nav*/path-for nav*/ui-routes page nil))
  ([page params]
   (nav*/path-for nav*/ui-routes page params)))

(defn api-path-for
  ([page]
   (nav*/path-for nav*/api-routes page nil))
  ([page params]
   (nav*/path-for nav*/api-routes page params)))

(defonce ^:private history
  (let [history (pushy/pushy (comp store/dispatch (partial conj [:router/navigate])) match-route)]
    (pushy/start! history)
    history))

(defn ^:private navigate* [history page params]
  (pushy/set-token! history (path-for page params)))

(defn ^:private nav-and-replace* [history page params]
  (pushy/replace-token! history (path-for page params)))

(defn navigate!
  ([page] (navigate* history page nil))
  ([page params]
   (navigate* history page params)
   nil))

(defn go-to! [path]
  (set! (.-pathname (.-location js/window)) path)
  nil)

(defn nav-and-replace!
  ([page] (nav-and-replace* history page nil))
  ([page params]
   (nav-and-replace* history page params)
   nil))
