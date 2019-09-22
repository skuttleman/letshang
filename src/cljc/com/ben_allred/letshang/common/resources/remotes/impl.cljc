(ns com.ben-allred.letshang.common.resources.remotes.impl
  (:require
    [com.ben-allred.letshang.common.resources.core :as res]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.colls :as colls]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as vp])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(defprotocol IRemote
  (success? [this])
  (ready? [this])
  (fetch! [this])
  (hydrated? [this])
  (persist! [this model]))

(def ^:private -ready?
  (comp #{:success :error} first))

(def ^:private -success?
  (comp #{:success} first))

(defrecord Remote [fetch persist reaction success-message error-message]
  IRemote
  (success? [_]
    (boolean (-success? @reaction)))
  (ready? [_]
    (boolean (-ready? @reaction)))
  (fetch! [this]
    (if (and fetch (not (hydrated? this)))
      (store/dispatch (fetch))
      (v/resolve)))
  (hydrated? [_]
    (some? (colls/third @reaction)))
  (persist! [_ model]
    (if persist
      (-> (persist model)
          (store/dispatch)
          (v/peek (res/toast-success (or success-message "Success"))
                  (res/toast-error (or error-message "Something went wrong"))))
      (v/reject {:message "Remote cannot be persisted" :data model})))

  vp/IPromise
  (then [this on-success on-error]
    (v/create (fn [resolve _]
                (letfn [(handle! [[status value]]
                          (resolve (case status
                                     :success (on-success value)
                                     :error (on-error value))))]
                  (if (ready? this)
                    (handle! @reaction)
                    (do (add-watch reaction (gensym) (fn [k _ _ result]
                                                       (when (-ready? result)
                                                         (remove-watch reaction k)
                                                         (handle! result))))
                        (fetch! this)))))))

  IDeref
  #?(:clj  (deref [_] (second @reaction))
     :cljs (-deref [_] (second @reaction))))

(defn create [{:keys [error-message fetch persist reaction success-message]}]
  (->Remote fetch persist reaction success-message error-message))
