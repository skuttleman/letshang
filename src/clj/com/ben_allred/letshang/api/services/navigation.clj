(ns com.ben-allred.letshang.api.services.navigation
  (:require
    [com.ben-allred.letshang.common.services.navigation :as nav*]))

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
