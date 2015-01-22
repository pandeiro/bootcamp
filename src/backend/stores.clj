(ns backend.stores
  (:require [environ.core :refer [env]]))

(def redis
  {:pool {}
   :spec {:host       (or (env :redis-host)     "127.0.0.1")
          :port       (or (env :redis-port)     6379)
          :password   (or (env :redis-password) nil)
          :timeout-ms (or (env :redis-timeout)  6000)
          :db         (or (env :redis-db)       1)}})

;;;
;;; Redis connections use this
;;;
(defmacro with-redis [& body]
  `(car/wcar redis ~@body))
