(ns com.ben-allred.letshang.ui.services.store.actions
  (:require
    [com.ben-allred.letshang.common.services.http :as http]
    [com.ben-allred.letshang.common.utils.chans :as ch]
    [com.ben-allred.letshang.common.utils.keywords :as keywords]
    [com.ben-allred.letshang.ui.services.navigation :as nav]))

(defn ^:private request* [request dispatch kwd-ns]
  (dispatch [(keywords/namespaced kwd-ns :request)])
  (-> request
      (ch/peek* (fn [[status response]]
                  (dispatch [(keywords/namespaced kwd-ns status) response])))))

(defn fetch-hangouts [[dispatch]]
  (-> (nav/path-for :api/hangouts)
      (http/get)
      (request* dispatch :hangouts)))
