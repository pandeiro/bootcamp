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

;;; Broadcast

(defn broadcast-if-changed! [k r o n]
  (when (= k :broadcast)
    (when (not= o n)
      (info "Data changed, queueing broadcast to sockets")
      (async/put! data-changed :dummy-value-not-important))))

(add-watch repos :broadcast broadcast-if-changed!)
(add-watch repo-info :broadcast broadcast-if-changed!)

;;; Logging

(defn- log-new-repos [_ _ old new]
  (let [diff (set/difference new old)]
    (when (not-empty diff)
      (doseq [{:keys [user repo]} diff]
        (info "Added repository %s" (str user "/" repo))))))

(add-watch repos :info log-new-repos)

(defn- log-new-repo-info [_ _ old new]
  (let [[_ new-stuff _] (data/diff old new)]
    (when (pos? (count new-stuff))
      (info "Added repo info: %s"
            (s/join ", " (map #(str (:user %) "/" (:repo %))
                              (keys new-stuff)))))))

(add-watch repo-info :info log-new-repo-info)
