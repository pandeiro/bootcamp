(ns frontend.queues.events
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljs.core.async :as async :refer [<! chan]]
   [frontend.net.xhr :as xhr]
   [frontend.net.socket :as ws]))

(defn event-loop [app-state]
  (let [tap (chan)]
    (async/tap (:read-events @app-state) tap)
    (go-loop []
      (let [[event data] (<! tap)]
        (case event
          :repo-info-request
          (xhr/retrieve-github-repo-info-data app-state data)
          :repo-info-added
          (ws/send! [:cached-repo-info-data data])
          :data-changed
          (xhr/retrieve-data app-state)
          nil)
        (recur)))))

