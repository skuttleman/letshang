(ns com.ben-allred.letshang.ui.services.navigation
  (:require
    [com.ben-allred.letshang.common.services.navigation :as nav*]
    [com.ben-allred.letshang.common.utils.dom :as dom]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.ui.services.store.core :as store]
    [pushy.core :as pushy]))

(defn match-route [path]
  (nav*/match-route nav*/app-routes path))

(defn path-for
  ([page]
   (nav*/path-for nav*/app-routes page nil))
  ([page params]
   (nav*/path-for nav*/app-routes page params)))

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

(defn go-to! [url]
  (.assign (.-location dom/window) url)
  nil)

(defn nav-and-replace!
  ([page] (nav-and-replace* history page nil))
  ([page params]
   (nav-and-replace* history page params)
   nil))
