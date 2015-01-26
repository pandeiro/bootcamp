(ns frontend.session
  (:require-macros [frontend.static :as static])
  (:require [cljs.core.async :as async :refer [<! chan]]
            [reagent.core :as r]
            [frontend.store :refer [store watch-and-store]]))

(def app-state
  "Application state holding atom.

  Holds data (which is cached to localStorage), along
  with communications channels and static, environment-
  sensitive configuration."
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

;;
;; Helpers
;;
(defn new-client-id []
  (str
   (.getTime (js/Date.)) "-"
   (apply str (repeatedly 10 #(first (shuffle (range 10)))))))


