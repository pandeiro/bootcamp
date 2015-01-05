(ns frontend.views.feed)

(defn feed [app-state]
  (if-not (:user @app-state)
    [:div "Look how nice it is!"]
    [:div.feed]))

