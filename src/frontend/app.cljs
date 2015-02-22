(ns frontend.app
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   cljsjs.react
   [cljs.core.async :as async :refer [<! chan]]
   [reagent.core :as r]
   [frontend.util.debug :as debug]
   [frontend.session.state :refer [app-state]]
   [frontend.net.xhr :as xhr]
   [frontend.net.socket :as ws]
   [frontend.queues.github :as gh]
   [frontend.views.main :as views]
   [frontend.util.helpers :refer [once new-client-id]]))

(def root (.getElementById js/document "root"))

(defn init []
  (when-not (get-in @app-state [:data :client-id])
    (swap! app-state assoc-in [:data :client-id] (new-client-id)))

  (async/put! (:write-events @app-state) [:init (js/Date.)])

  (once debug/debug-events  app-state)
  (once xhr/retrieve-data   app-state)
  (once xhr/retrieve-stats  app-state)
  (once ws/connect          app-state)

  (r/render [views/main app-state] root))

