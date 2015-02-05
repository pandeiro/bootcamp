(ns backend.stores
  (:require
   [clojure.core.async :as async]
   [clojure.set :as set]
   [clojure.data :as data]
   [clojure.string :as s]
   [environ.core :refer [env]]
   [backend.socket :as ws]
   [backend.logging :refer [info warn]]
   [backend.queues :refer [data-changed]]))

(info "Loading backend data stores")

(defonce
  ^{:doc
    "Stores all the GitHub repositories that match the Boot-finding heuristic in a set."}
  repos
  (atom #{}))

(defonce
  ^{:doc
    "Stores a map of repositories to the info available about them via the GitHub API"}
  repo-info
  (atom {}))


;;; Broadcast to data-changed

(defn broadcast-if-changed! [k r o n]
  (when (= k :broadcast)
    (when (not= o n)
      (async/put! data-changed :dummy-value-not-important))))

(add-watch repos :broadcast broadcast-if-changed!)
(add-watch repo-info :broadcast broadcast-if-changed!)
