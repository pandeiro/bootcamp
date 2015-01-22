(ns frontend.session
  (:require [cljs.core.async :as async :refer [<! chan]]
            [reagent.core :as r]
            [frontend.store :refer [store watch-and-store]]))

(def app-state
  (let [events (chan)]
    (r/atom
     {:user         (or (:user @store)   nil)
      :logins       (or (:logins @store) #{})
      :data         (or (:data @store)   {})
      :write-events events
      :read-events  (async/mult events)})))

(-> app-state
  (watch-and-store [:user])
  (watch-and-store [:logins])
  (watch-and-store [:data]))

(defn assoc-user! [user]
  (swap! app-state assoc :user user)
  (swap! app-state update-in [:logins] conj user))

(defn dissoc-user! [user]
  (swap! app-state assoc :user nil))
