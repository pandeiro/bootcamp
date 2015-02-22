(ns frontend.net.xhr
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [<! chan]]
            [clojure.set :as set]
            [clojure.string :as s]
            [cljs-http.client :as http]
            [frontend.util.helpers :as u]))

(defn- dev-swap-hostname [url]
  (when (not (neg? (.indexOf url "localhost")))
    (s/replace url #"localhost" (.-hostname js/window.location))))

;;
;; Boot-camp API
;;

(defn merge-if-newer [existing [entry-key entry-data]] ; logic copied from backend.services.github
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
          (let [new-repo-info (get-in response [:body :repo-info])]
            (doseq [info new-repo-info]
              (swap! app-state update-in [:data :repo-info]
                     merge-if-newer
                     info)))
          (let [all-users
                (map (fn [[repo repo-info-data]]
                       {:user (:user repo)
                        :info repo-info-data})
                     (get-in response [:body :repo-info]))
                missing-users
                (remove
                 (fn [{:keys [user info]}]
                   (let [existing-users (set (keys (get-in @app-state [:data :users])))]
                     (existing-users user)))
                 all-users)]
            (doseq [{:keys [user info]} missing-users]
              (let [user-info (get-in info [:owner])]
                (swap! app-state assoc-in [:data :users user] user-info)))))))))

(defn retrieve-stats
  "Retrieves statistics about the number of boot repos"
  [app-state]
  (let [{:keys [host api]} (get-in @app-state [:config :urls])
        data-url           (str (dev-swap-hostname host) api "/stats")]
    (go
      (let [response (<! (http/get data-url {:with-credentials? false}))]
        (when (= 200 (:status response))
          (swap! app-state update-in [:data :stats] merge
                 (get-in response [:body :stats])))))))

;;
;; Third-party
;;
(defn retrieve-github-repo-info-data [app-state {:keys [user repo] :as this-repo}]
  (let [data-url (str "https://api.github.com/repos/" user "/" repo)]
    (go
      (let [response (<! (http/get data-url {:with-credentials? false}))]
        (when (= (:status response) 200)
          (let [body (get-in response [:body])]
            (async/put! (:write-events @app-state) [:repo-info-added [{this-repo body}]])
            (swap! app-state assoc-in
                   [:data :repo-info this-repo]
                   body)
            (swap! app-state assoc-in
                   [:data :users user]
                   (get-in body [:owner]))))))))
