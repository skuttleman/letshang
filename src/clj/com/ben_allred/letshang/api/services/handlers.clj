(ns com.ben-allred.letshang.api.services.handlers
  (:require
    [com.ben-allred.letshang.api.services.middleware :as middleware]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [compojure.core :as compojure]))

(defn ^:private wrap* [meta handler]
  `(-> ~handler
       (compojure/wrap-routes #'middleware/conform-params (:transformer ~meta))))

(defmacro context [path args & routes]
  (wrap* (meta args) `(compojure/context ~path ~args ~@routes)))

(defmacro ANY [path args & body]
  (wrap* (meta args) `(compojure/ANY ~path ~args ~@body)))

(defmacro DELETE [path args & body]
  (wrap* (meta args) `(compojure/DELETE ~path ~args ~@body)))

(defmacro GET [path args & body]
  (wrap* (meta args) `(compojure/GET ~path ~args ~@body)))

(defmacro HEAD [path args & body]
  (wrap* (meta args) `(compojure/HEAD ~path ~args ~@body)))

(defmacro OPTIONS [path args & body]
  (wrap* (meta args) `(compojure/OPTIONS ~path ~args ~@body)))

(defmacro PATCH [path args & body]
  (wrap* (meta args) `(compojure/PATCH ~path ~args ~@body)))

(defmacro POST [path args & body]
  (wrap* (meta args) `(compojure/POST ~path ~args ~@body)))

(defmacro PUT [path args & body]
  (wrap* (meta args) `(compojure/PUT ~path ~args ~@body)))
