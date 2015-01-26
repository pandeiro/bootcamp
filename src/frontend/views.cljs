(ns frontend.views
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [chan <!]]
            [goog.string.format]
            [reagent.core :as r]
            [cljs-http.client :as http]
            [frontend.session :as session]
            [frontend.socket :as ws]))

(def format goog.string/format)

;;; Network

(defn- retrieve-boot-svg [app-state]
  (let [path [:data :assets :boot]]
    (when-not (get-in @app-state path)
      (go (let [svg (:body (<! (http/get "img/boot.svg")))]
            (swap! app-state assoc-in path svg))))))

(defn- github-icon [avatar-url]
  (str avatar-url "&s=40"))

;;; Views

(defn repo-link [user repo]
  [:a {:href (format "https://github.com/%s/%s" user repo)}
   (format "%s/%s" user repo)])

(def boot-repo-columns
  [:user :repo :updated :build :stars])

(defn- rel-time [x] x) ;; TODO: momentjs

(defn github-avatar [url user size]
  [:div
   [:a {:href (format "https://github.com/%s" user)}
    [:img
     {:src (github-icon url) :width size :height size :title user :alt user}]]])

(defn boot-repo-user [data]
  (let [user (get-in data [:repo :user])
        url  (get-in @session/app-state [:data :users user :avatar_url])]
    [:td.user
     (if url
       [github-avatar url user 25]
       user)]))

(defn boot-repo-build [data]
  [:td.build "build.boot (fixme)"])

(defn boot-repo-reponame [data]
  [:td.repo (get-in data [:repo :repo])])

(defn boot-repo-updated [data]
  [:td.updated (rel-time (get-in data [:repo-info :updated_at]))])

(defn boot-repo-stars [data]
  [:td.stars (get-in data [:repo-info :stargazers_count])])

(def boot-repo-cells
  {:build   boot-repo-build
   :user    boot-repo-user
   :repo    boot-repo-reponame
   :updated boot-repo-updated
   :stars   boot-repo-stars})

(defn- mount-boot-logo-svg [parent svg-src size]
  (let [el (.querySelector (r/dom-node parent) ".svg")]
    (when el
      (set! (.-innerHTML el) svg-src)
      (let [svg (aget (.getElementsByTagName el "svg") 0)]
        (.setAttribute svg "width" size)
        (.setAttribute svg "height" size)))))

(defn on-repo-click [data]
  (fn [_]
    (if-let [repo-info (not-empty (get-in data [:repo-info]))]
      (js/alert (pr-str repo-info))
      (session/put-event! [:repo-info-request (get-in data [:repo])]))))

(defn boot-repo [repo-data boot-svg]
  (let [logo-size 90]
    (r/create-class
     {:render
      (fn [_]
        [:tr.repo {:on-click (on-repo-click repo-data)}
         (for [col boot-repo-columns]
           (let [boot-repo-cell (get boot-repo-cells col)]
             ^{:key (str (get-in repo-data [:repo :user])
                         (get-in repo-data [:repo :repo])
                         col)}
             [boot-repo-cell repo-data]))])
      :component-did-mount
      (fn [this]
        (mount-boot-logo-svg this boot-svg logo-size))})))

(defn- get-repo-info [repo-info repo]
  (get repo-info repo))

(defn boot-repos-list [app-state]
  (retrieve-boot-svg app-state)
  (fn [_]
    (let [boot-svg  (get-in @app-state [:data :assets :boot])
          repos     (get-in @app-state [:data :repos])
          repo-info (get-in @app-state [:data :repo-info])]
      (.log js/console "rendering boot-repos-list")
      [:div.repos.shortstack
       [:table
        [:thead
         [:tr
          (for [col boot-repo-columns]
            ^{:key (str "heading" col)}
            [:th (name col)])]]
        [:tbody
         (for [repo repos]
           ^{:key (str repo)}
           [boot-repo
            {:repo repo, :repo-info (get-repo-info repo-info repo)}
            boot-svg])]]])))

(defn search [app-state]
  (let [repos (get-in @app-state [:data :repos])]
    [:div.search.pure-form
     [:p.shortstack
      (format "Search %d enlisted builds: " (count repos))]
     [:input.shortstack
      {:placeholder
       "eg, \"with-pre-wrap\" or \"uberjar\" or \"alandipert\""
       :on-key-down
       (fn [e]
         (when (= 13 (.-keyCode e))
           (.log js/console "SEARCH!")))}]]))

(defn main [app-state]
  [:div.container
   [search app-state]
   [boot-repos-list app-state]])
