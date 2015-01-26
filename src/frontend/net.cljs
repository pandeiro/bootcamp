(ns frontend.net
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [<! chan]]
            [clojure.set :as set]
            [cljs-http.client :as http]))

(defn retrieve-data
  "Retrieves data on repositories from the API and merges the results into
  app-state."
  [app-state]
  (let [{:keys [host api]} (get-in @app-state [:config :urls])
        data-url           (str host api "/repos")]
    (go
      (let [response (<! (http/get data-url {:with-credentials? false}))]
        (when (= 200 (:status response))
          (swap! app-state update-in [:data :repos]
                 set/union
                 (get-in response [:body :repos]))
          (swap! app-state update-in [:data :repo-info]
                 ;; TODO: here you would want the same :updated_at check
                 ;; logic that is used on the server-side so that a stale
                 ;; server-side version doesn't overwrite a newer local one
                 (partial merge-with merge)
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

;; (def urls
;;   {:user "http://localhost:9090/user"
;;    :auth "http://localhost:9090/auth"})

;; (defn announce! [app-state k data]
;;   (async/put! (:write-events @app-state) [k data]))

;; (defn- url-lookup [x]
;;   (if-not (keyword? x)
;;     x
;;     (let [url (cljs.core/get urls x)]
;;       (assert url "URL keyword wasn't found")
;;       url)))

;; (defn request [{:keys [method url data]}]
;;   (let [resolved-url (url-lookup url)
;;         methods {:get xhr/get, :post xhr/post}
;;         request ((cljs.core/get methods method)
;;                  resolved-url
;;                  (merge {:with-credentials? false}
;;                         (when (= :post method)
;;                           {:edn-params data})))]
;;     (announce! app-state :request {:method method, :url url, :data data})
;;     (go (announce! app-state :response (assoc (<! request) :url url, :data data)))))

;; (defn get [url]
;;   (request {:method :get, :url url}))

;; (defn post [url data]
;;   (request {:method :post, :url url, :data data}))
