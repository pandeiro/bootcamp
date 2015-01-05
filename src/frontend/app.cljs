(ns frontend.app
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :as async :refer [<! chan]]
            [reagent.core :as r]
            [alandipert.storage-atom :refer [local-storage]]
            [frontend.debug :refer [debug-events]]
            [frontend.session :refer [app-state]]
            [frontend.net :as xhr]
            [frontend.views.userbar :refer [user-status-bar]]
            [frontend.views.update :refer [post-update]]
            [frontend.views.feed :refer [feed]]))

(defn view [app-state]
  [:div.container
   [user-status-bar app-state]
   [post-update app-state]
   [feed app-state]])

(def root (.getElementById js/document "root"))

(defn event-loop [app-state]
  (let [tap (chan)]
    (async/tap (:read-events @app-state) tap)
    (go-loop []
      (let [[name data] (<! tap)]
        (.log js/console "event-loop" (pr-str [name data]))
        (case name
          :signin-send-click (xhr/post :auth (:data data))
          nil)
        (recur)))))

(defn init []
  ;;(debug-events app-state)
  (event-loop app-state)
  (r/render [view app-state] root))

(.addEventListener js/window "DOMContentLoaded" init)
