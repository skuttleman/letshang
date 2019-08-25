(ns com.ben-allred.letshang.common.utils.chans
  (:refer-clojure :exclude [peek resolve])
  (:require
    [clojure.core.async :as async]
    [clojure.core.async.impl.protocols :as async.protocols]))

(defn ^:private handle [cb & args]
  (try (apply cb args)
       (catch #?(:clj Throwable :cljs :default) ex
         (async/go [:error ex]))))

(defn resolve
  ([]
   (resolve nil))
  ([value]
   (async/to-chan (repeat [:success value]))))

(defn reject
  ([]
   (reject nil))
  ([err]
   (async/to-chan (repeat [:error err]))))

(defn ^:private with-status [ch st cb]
  (let [next-ch (async/chan 10)]
    (async/go
      (let [[status value :as result] (async/<! ch)]
        (if (= st status)
          (let [next (handle cb value)]
            (async/onto-chan next-ch (repeat (if (satisfies? async.protocols/ReadPort next)
                                               (async/<! next)
                                               [:success next]))))
          (async/onto-chan next-ch (repeat result)))))
    next-ch))

(defn catch [ch on-error]
  (with-status ch :error on-error))

(defn then
  ([ch on-success]
   (with-status ch :success on-success))
  ([ch on-success on-error]
   (-> ch
       (then on-success)
       (catch on-error))))

(defn peek
  ([ch cb]
   (peek ch
         (comp cb (partial conj [:success]))
         (comp cb (partial conj [:error]))))
  ([ch on-success on-error]
   (let [next-ch (async/chan 10)]
     (async/go
       (let [[status result :as response] (async/<! ch)]
         (cond
           (and on-success (= :success status))
           (handle on-success result)

           (and on-error (= :error status))
           (handle on-error result))
         (async/onto-chan next-ch (repeat response))))
     next-ch)))

(defn finally [ch cb]
  (let [next-ch (async/chan 10)]
    (async/go
      (let [result (async/<! ch)]
        (let [next (handle cb)]
          (async/onto-chan next-ch (repeat (if (satisfies? async.protocols/ReadPort next)
                                             (-> next
                                                 (then (fn [_] (async/go result)))
                                                 (async/<!))
                                             result))))))
    next-ch))

(defn all [chs]
  (reduce (fn [result-ch ch]
            (then result-ch (fn [results]
                              (then ch #(conj results %)))))
          (resolve [])
          chs))
