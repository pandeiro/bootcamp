(ns frontend.net
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [<! chan]]
            [clojure.set :as set]
            [clojure.string :as s]
            [cljs-http.client :as http]
            [frontend.util :as u]))

(defn- dev-swap-hostname [url]
  (when (not (neg? (.indexOf url "localhost")))
    (s/replace url #"localhost" (.-hostname js/window.location))))

;; logic copied from backend.services.github
(defn merge-if-newer [existing [entry-key entry-data]]
  (let [existing-entry (get existing entry-key)]
    (if (empty? existing-entry)
      (merge existing (vector entry-key entry-data))
      (let [updated-existing (get existing-entry :updated_at)
            updated-new (get entry-data :updated_at)]
        (if (pos? (u/compare-iso-dates updated-new updated-existing))
          (merge existing (vector entry-key entry-data))
          existing)))))

(defn retrieve-data
  "Retrieves data on repositories from the API and merges the results into
  app-state."
  [app-state]
  (let [{:keys [host api]} (get-in @app-state [:config :urls])
        data-url           (str (dev-swap-hostname host) api "/repos")]
    (go
      (let [response (<! (http/get data-url {:with-credentials? false}))]
        (when (= 200 (:status response))
          (swap! app-state update-in [:data :repos]
                 set/union
                 (get-in response [:body :repos]))
          (swap! app-state update-in [:data :repo-info]
                 merge-if-newer
                 (get-in response [:body :repo-info]))
          (let [users (map (fn [[repo ri]]
                             {:user (:user repo)
                              :info ri})
                           (get-in response [:body :repo-info]))]
            (doseq [{:keys [user info]}
                    (remove
                     (fn [{:keys [user info]}]
                       (let [existing-users (set (keys (get-in @app-state [:data :users])))]
                         (existing-users user)))
                     users)]
              (swap! app-state assoc-in [:data :users user] info))))))))

(defn retrieve-github-repo-data [app-state {:keys [user repo] :as this-repo}]
  (let [data-url (str "https://api.github.com/repos/" user "/" repo)]
    (go
      (let [response (<! (http/get data-url {:with-credentials? false}))]
        (when (= (:status response) 200)
          (async/put! (:write-events @app-state) [:repo-info-added this-repo])
          (swap! app-state assoc-in
                 [:data :repo-info this-repo]
                 (get-in response [:body]))
          (swap! app-state assoc-in
                 [:data :users user]
                 (get-in response [:body :owner])))))))
