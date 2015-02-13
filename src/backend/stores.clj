(ns backend.stores
  (:require
   [clojure.core.async :as async]
   [clojure.set :as set]
   [clojure.data :as data]
   [clojure.string :as s]
   [environ.core :refer [env]]
   [backend.socket :as ws]
   [backend.logging :refer [info warn]]
   [backend.queues :refer [data-changed]]
   [backend.util :as u]))

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

(defonce
  ^{:doc
    "Stores statistics about the number of repos and users"}
  stats
  (atom {}))

;;; Broadcast to data-changed

(defn broadcast-if-changed! [k r o n]
  (when (= k :broadcast)
    (when (not= o n)
      (async/put! data-changed :dummy-value-not-important))))

(add-watch repos :broadcast broadcast-if-changed!)
(add-watch repo-info :broadcast broadcast-if-changed!)

;;; Stats

(defn update-stat-count! [k r o n]
  (swap! stats assoc (u/now-as-date-string) {:repos (count n), :users (count (set (map :user n)))}))

(add-watch repos :stats update-stat-count!)
