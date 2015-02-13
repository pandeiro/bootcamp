(ns frontend.app
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   cljsjs.moment
   cljsjs.react
   [clojure.data :refer [diff]]
   [cljs-http.client :as http]
   [cljs.core.async :as async :refer [<! chan]]
   [reagent.core :as r]
   [shodan.console :as console :include-macros true]
   [frontend.debug :as debug]
   [frontend.session :as session :refer [app-state new-client-id]]
   [frontend.net :as net]
   [frontend.socket :as ws]
   [frontend.github :as gh]
   [frontend.views :as views]
   [frontend.util :refer [once]]))

(def root (.getElementById js/document "root"))

(defonce event-loop
  (let [tap (chan)]
    (async/tap (:read-events @app-state) tap)
    (go-loop []
      (let [[event data] (<! tap)]
        (case event
          :repo-info-request
          (net/retrieve-github-repo-info-data app-state data)
          :repo-info-added
          (ws/send! [:cached-repo-info-data data])
          :data-changed
          (net/retrieve-data app-state)
          nil)
        (recur)))))

(defn init []
  (when-not (get-in @app-state [:data :client-id])
    (swap! app-state assoc-in [:data :client-id] (new-client-id)))

  (async/put! (:write-events @app-state) [:init (js/Date.)])

  (once debug/debug-events  app-state)
  (once net/retrieve-data   app-state)
  (once gh/repo-info-worker app-state)
  (once ws/connect          app-state)

  (r/render [views/main app-state] root))

