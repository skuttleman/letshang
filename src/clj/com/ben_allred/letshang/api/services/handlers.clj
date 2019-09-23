(ns com.ben-allred.letshang.api.services.handlers
  (:require
    [com.ben-allred.letshang.api.services.html :as html]
    [com.ben-allred.letshang.api.services.middleware :as middleware]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [ring.util.mime-type :as mime]
    [ring.util.response :as response]
    [com.ben-allred.letshang.common.utils.maps :as maps]))

(defn ^:private add-mime-type [response path]
  (if-let [mime-type (mime/ext-mime-type path {})]
    (response/content-type response mime-type)
    response))

(defn ^:private wrap-meta* [{:keys [transformer request-spec]} handler]
  (-> handler
      (#'middleware/with-request-conforming transformer)
      (#'middleware/with-request-validation request-spec)))

(defn ^:private with-meta* [handler m handlers]
  (-> m
      (meta)
      (merge (meta handlers)
             (meta handler))
      (wrap-meta* handler)))

(defn resources [{:keys [uri]}]
  (some-> (response/resource-response (str "public" uri))
          (add-mime-type uri)))

(defn wrap-meta [m]
  (maps/update-all m (fn [handlers]
                       (if (map? handlers)
                         (maps/update-all handlers with-meta* m handlers)
                         (-> handlers
                             (cond-> (var? handlers) (var-get))
                             (vary-meta (partial merge (meta m))))))))

(defn home [req]
  [:http.status/ok
   (-> req
       (select-keys #{:auth/sign-up :auth/user :query-string :uri})
       (html/render))
   {"content-type" "text/html"}])

(def health
  (constantly [:http.status/ok {:a :ok}]))
