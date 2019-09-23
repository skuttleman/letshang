(ns com.ben-allred.letshang.api.services.navigation
  (:require
    [com.ben-allred.letshang.common.services.navigation :as nav*]))

(defn match-route [path]
  (nav*/match-route nav*/app-routes path))

(defn path-for
  ([page]
   (nav*/path-for nav*/app-routes page nil))
  ([page params]
   (nav*/path-for nav*/app-routes page params)))

(defn navigate!
  ([page] nil)
  ([page params]
   nil))

(defn go-to! [url]
  nil)

(defn nav-and-replace!
  ([page] nil)
  ([page params]
   nil))
