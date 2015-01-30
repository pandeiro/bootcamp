(ns backend.services.github
  (:require
   [clojure.string :as s]
   [clojure.core.async :as async :refer [go go-loop chan <! >!]]
   [org.httpkit.client :as http]
   [net.cgrand.enlive-html :as enlive]
   [backend.stores :as stores]
   [backend.logging :refer [info warn]]
   [backend.queues :refer [gh-html-queue
                           gh-req-queue
                           gh-repos-queue
                           gh-repo-info-queue]]
   [backend.util :as u]))

;;; Network

(def search-url
  "URL used for performing GitHub code searches for Boot projects.
  Looks for the .boot extension and the presence of 'deftask'
  and 'set-env' in the code."
  (str "https://github.com/search?"
       "utf8=%E2%9C%93&"
       "q=set-env+extension%3Aboot&"
       "type=Code&"
       "ref=searchresults"))

(def ua
  (str "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
       "(KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36"))

(defn github-search-request
  [& [{:keys [page]}]]
  (http/request {:url (str search-url "&p=" (or page 1))
                 :user-agent ua}))

;;; Processing

(defn- parse-int [x] (try (Integer/parseInt x) (catch Exception e nil)))

(defn- enliven [html search-path]
  (enlive/select (enlive/html-snippet html)
    (if (vector? search-path)
      search-path
      (vector search-path))))

(defn- parse-repo-link [s]
  (let [[user repo] (s/split s #"/")]
    {:user user, :repo repo}))

(def pagination-path [:.pagination :a])
(def repo-link-path [:.code-list :p.title :a])

(defn count-boot-repo-pages [html]
  (let [selection (enliven html pagination-path)]
    (apply max
           (->> selection
             (map enlive/text)
             (map parse-int)
             (remove nil?)))))

(defn get-boot-repos
  "Given a GitHub search results page as HTML, returns a sequence of
  maps with :user and :repo keys"
  [html]
  (->> (enliven html repo-link-path)
    (map enlive/text)
    (partition 2)
    (map first)
    (map parse-repo-link)))

;;; Search Init

(defn search-github []
  (let [first-page @(github-search-request)]
    (if-not (= 200 (:status first-page))
      (warn "Error: Initial GitHub search request failed with status %d"
            (:status first-page))
      (let [html       (:body first-page)
            num-pages  (count-boot-repo-pages html)]
        (when html
          (async/put! gh-html-queue html))
        (when num-pages
          (doseq [page (range 2 (inc num-pages))]
            (async/put! gh-req-queue [:search-results page])))))))

;;; Persistence

(defn add-repo! [repo]
  (if (not-empty repo)
    (swap! stores/repos conj repo)
    (warn "Tried to add invalid repo: %s" (str repo))))

(defn merge-if-newer [existing [entry-key entry-data]]
  (let [existing-entry (get existing entry-key)]
    (if (empty? existing-entry)
      (merge existing (vector entry-key entry-data))
      (let [updated-existing (get existing-entry :updated_at)
            updated-new (get entry-data :updated_at)]
        (if (pos? (u/compare-iso-dates updated-new updated-existing))
          (merge existing (vector entry-key entry-data))
          existing)))))

(defn add-repo-info!
  "Merges a repo-info map into the existing store.

  When a map entry is already present in the store, it is only merged
  if its :updated_at key is later than the existing entry's."
  [repo-info]
  (info "Adding repo info: %s" (pr-str (first (keys repo-info))))
  (swap! stores/repo-info merge-if-newer repo-info))

;;; Worker

(defn start-worker []
  (def worker
    (future
      (info "GitHub worker started...")
      ;; HTML page processing
      (go-loop []
        (let [html (<! gh-html-queue)]
          (doseq [repo (get-boot-repos html)]
            (when repo
              (async/put! gh-repos-queue repo)))
          (recur)))
      ;; Repos processing
      (go-loop []
        (add-repo! (<! gh-repos-queue))
        (recur))
      ;; Repo info processing
      (go-loop []
        (add-repo-info! (<! gh-repo-info-queue))
        (recur))
      ;; GH http request throttling
      (go-loop []
        (let [[topic data] (<! gh-req-queue)]
          (case topic
            :search-results
            (let [search-results-page data
                  response @(github-search-request {:page search-results-page})]
              (info "Requested: GitHub search results page %d" search-results-page)
              (if-not (= 200 (:status response))
                (warn "Error: GitHub search request returned status %d"
                      (:status response))
                (when-let [html (:body response)]
                  (async/put! gh-html-queue html))))
            nil)
          (<! (async/timeout (* 1000 60 10))) ; wait 10 minutes between requests
          (recur)))
      ;; GH search scheduling
      (go-loop []
        (info "Began GitHub search")
        (search-github)
        (<! (async/timeout (* 1000 60 60 24)))
        (recur)))))

(defn stop-worker []
  (info "GitHub worker stopping...")
  (future-cancel worker))
