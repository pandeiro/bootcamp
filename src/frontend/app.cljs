(ns frontend.app
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   cljsjs.moment
   cljsjs.react
   [cljs-http.client :as http]
   [cljs.core.async :as async :refer [<! chan]]
   [reagent.core :as r]
   [frontend.debug :refer [debug-events]]
   [frontend.session :as session :refer [app-state new-client-id]]
   [frontend.net :as net]
   [frontend.socket :as ws]
   [frontend.github :as gh]
   [frontend.views :as views]))

(def root (.getElementById js/document "root"))

(defonce event-loop
  (let [tap (chan)]
    (async/tap (:read-events @app-state) tap)
    (go-loop []
      (let [[event data] (<! tap)]
        (case event
          :repo-info-request
          (net/retrieve-github-repo-data app-state data)
          nil)
        (recur)))))

(defn init []
  (when-not (get-in @app-state [:data :client-id])
    (swap! app-state assoc-in [:data :client-id] (new-client-id)))
  (debug-events app-state)
  (net/retrieve-data app-state)
  (gh/repo-info-worker app-state)
  (ws/connect app-state)
  (r/render [views/main app-state] root))

