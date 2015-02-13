(ns frontend.views
  (:require
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
  [:div.container
   [boot-repos-sidebar app-state]
   [boot-repos-content app-state]])
