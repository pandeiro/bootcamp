(ns frontend.views.main
  (:require
   [shodan.console :as console :include-macros true]
   [reagent.core :as r]
   [frontend.queues.events :as ev]
   [frontend.queues.github :as gh]
   [frontend.views.stats :refer [boot-stats]]
   [frontend.views.list :refer [boot-repos-list]]))

(defn boot-repos-content [app-state]
  [:div.content
   [boot-repos-list app-state]])

(defn boot-repos-sidebar [app-state]
  [:div.sidebar
   [:img {:src "img/sargeant.svg" :height 180}]
   [:h1.londrina.all-caps "Boot Camp"]
   [boot-stats app-state]])

(defn main [app-state]
  (r/create-class
   {:reagent-render
    (fn [app-state]
      [:div.container
       [boot-repos-sidebar app-state]
       [boot-repos-content app-state]])
    :component-did-mount
    (fn [this]
      (console/log "main view :component-did-mount")
      (ev/event-loop app-state)
      (gh/repo-info-worker app-state))}))
