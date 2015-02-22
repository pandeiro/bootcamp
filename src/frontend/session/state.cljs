(ns frontend.session.state
  (:require-macros [frontend.macros.static :as static])
  (:require [cljs.core.async :as async :refer [<! chan]]
            [reagent.core :as r]
            [frontend.session.store :refer [store watch-and-store]]))

(defonce ^{:doc "
Application state holding atom.

Holds data (which is cached to localStorage), along
with communications channels and static, environment-
sensitive configuration."}
  app-state
  (let [events (chan)]
    (r/atom
     {:data         (or (:data @store) {})
      :write-events events
      :read-events  (async/mult events)
      :config       (static/read-config)})))

(-> app-state
  (watch-and-store [:data]))

;;
;; Events
;;
(defn put-event! [data]
  (async/put! (:write-events @app-state) data))




