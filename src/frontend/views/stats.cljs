(ns frontend.views.stats)

(defn boot-stats [app-state]
  (let [stats (get-in @app-state [:data :stats])
        repos (get-in @app-state [:data :repos])
        repos-count (count repos)
        users-count (count (set (map :user repos)))]
    [:div.stats
     [:h3 (str "Repos: " repos-count)]
     [:h3 (str "Users: " users-count)]]))

