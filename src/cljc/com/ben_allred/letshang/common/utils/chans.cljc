(ns com.ben-allred.letshang.common.utils.chans
  (:refer-clojure :exclude [peek resolve])
  (:require
    [clojure.core.async :as async]
    [clojure.core.async.impl.protocols :as async.protocols]))

(defn ^:private handle [cb & args]
  (try (apply cb args)
       (catch #?(:clj Throwable :cljs :default) ex
         (async/go [:error ex]))))

(defn ^:private with-status [ch st cb]
  (async/go
    (let [[status value :as result] (async/<! ch)]
      (if (= st status)
        (let [next (handle cb value)]
          (if (satisfies? async.protocols/ReadPort next)
            (async/<! next)
            [:success next]))
        result))))

(defn resolve
  ([]
   (resolve nil))
  ([value]
   (async/go [:success value])))

(defn reject
  ([]
   (reject nil))
  ([err]
   (async/go [:error err])))

(defn catch [ch on-error]
  (with-status ch :error on-error))

(defn then
  ([ch on-success]
   (with-status ch :success on-success))
  ([ch on-success on-error]
   (-> ch
       (then on-success)
       (catch on-error))))

(defn peek* [ch cb]
  (async/go
    (let [result (async/<! ch)]
      (handle cb result)
      result)))

(defn peek
  ([ch cb]
   (peek ch cb (constantly nil)))
  ([ch on-success on-error]
   (peek* ch (fn [[status value]]
               (if (= :success status)
                 (on-success value)
                 (on-error value))))))

(defn finally [ch cb]
  (async/go
    (let [result (async/<! ch)]
      (let [next (handle cb)]
        (if (satisfies? async.protocols/ReadPort next)
          (-> next
              (then (fn [_] (async/go result)))
              (async/<!))
          result)))))

(defmacro ->catch [ch binding & body]
  `(com.ben-allred.letshang.common.utils.chans/catch ~ch (fn [~binding] ~@body)))

(defmacro ->then [ch binding & body]
  `(com.ben-allred.letshang.common.utils.chans/then ~ch (fn [~binding] ~@body)))

(defmacro ->peek [ch binding & body]
  `(com.ben-allred.letshang.common.utils.chans/peek ~ch (fn [~binding] ~@body)))

(defmacro ->finally [ch & body]
  `(com.ben-allred.letshang.common.utils.chans/finally ~ch (fn [] ~@body)))
