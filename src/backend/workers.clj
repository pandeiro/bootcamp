(ns backend.workers
  (:require [clojure.core.async :as async :refer [go-loop chan <! >!]]))

(def jobs (chan))

(defn work! []
  (go-loop []
    (<! jobs)
    (recur)))
