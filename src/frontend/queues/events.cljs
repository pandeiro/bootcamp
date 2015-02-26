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
          (let [[repo resp] data
                user        (get-in repo [:user])
                user-data   (get-in resp [:owner])]
            (swap! app-state assoc-in [:data :repo-info repo] resp)
            (swap! app-state assoc-in [:data :users user] user-data)
            (ws/send! [:cached-repo-info-data [repo resp]]))

          :data-changed
          (let []
            (.log js/console ":data-changed")
            (xhr/retrieve-data app-state))

          nil)

        (recur)))))

