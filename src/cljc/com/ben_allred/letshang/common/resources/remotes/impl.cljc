(ns com.ben-allred.letshang.common.resources.remotes.impl
  (:require
    [com.ben-allred.letshang.common.resources.core :as res]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as vp])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(defprotocol IRemote
  (success? [this])
  (ready? [this])
  (persist! [this model]))

(defprotocol ICache
  (invalidate! [this]))

(defn ^:private deref* [match? [status value] fetch]
  (let [match? (match?)]
    (when (and fetch (or (= :init status) (not match?)))
      (store/dispatch (fetch)))
    (when match?
      value)))

(defn create [{:keys [error-message fetch invalidate! match? persist reaction success-message]
               :or {match? (constantly true)}}]
  (reify
    IRemote
    (success? [_]
      (and (match?) (= :success (first @reaction))))
    (ready? [_]
      (and (match?) (contains? #{:success :error} (first @reaction))))
    (persist! [_ model]
      (if persist
        (-> (persist model)
            (store/dispatch)
            (v/peek (res/toast-success (or success-message "Success"))
                    (res/toast-error (or error-message "Something went wrong"))))
        (v/reject {:message "Remote cannot be persisted" :data model})))

    ICache
    (invalidate! [this]
      (when invalidate!
        (store/dispatch (invalidate!)))
      this)

    vp/IPromise
    (then [this on-success on-error]
      (v/create (fn [resolve reject]
                  @this
                  (letfn [(handle! [[status value]]
                            (try (case status
                                   :success (resolve (on-success value))
                                   :error (resolve (on-error value)))
                                 (catch #?(:clj Throwable :cljs :default) ex
                                   (reject ex))))]
                    (if (ready? this)
                      (handle! @reaction)
                      (add-watch reaction (gensym) (fn [k _ _ result]
                                                     (when (ready? this)
                                                       (remove-watch reaction k)
                                                       (handle! result)))))))))

    IDeref
    #?(:clj  (deref [_] (deref* match? @reaction fetch))
       :cljs (-deref [_] (deref* match? @reaction fetch)))))
