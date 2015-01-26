(ns backend.logging
  (:require [clojure.string :as s]))

(defn- logger [level]
  (fn [& args]
    (println (apply format (str "[" (java.util.Date.) "] "
                                (s/upper-case (name level)) " " (first args))
                    (rest args)))))

(def info (logger :info))
(def warn (logger :warn))
