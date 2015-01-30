(ns frontend.views
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [chan <!]]
            [goog.string.format]
            [clojure.string :as s]
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
  [:repo :updated :stars])

(defn- rel-time [x]
  (.fromNow (js/moment x)))

(defn github-avatar [url user size]
  [:a
   {:href (format "https://github.com/%s" user)
    :target "_blank"}
   [:img {:src (github-icon url) :width size :height size :title user}]])

(defn boot-repo-user [data]
  (let [user (get-in data [:repo :user])
        url  (get-in @session/app-state [:data :users user :avatar_url])]
    [:div.repo-user (if url
                      [github-avatar url user 25]
                      user)]))

(defn boot-repo-name [data]
  (let [repo (get-in data [:repo :repo])
        user (get-in data [:repo :user])]
    [:div.repo-name
     [:a {:href (format "https://github.com/%s/%s" user repo)
          :target "_blank"}
      repo]]))

(defn boot-repo-repository [data]
  [:div.row-item.repo
   [boot-repo-user data]
   [boot-repo-name data]])

(defn boot-repo-updated [data]
  [:div.row-item.updated
   (rel-time (get-in data [:repo-info :updated_at]))])

(defn boot-repo-stars [data]
  [:div.row-item.stars
   (get-in data [:repo-info :stargazers_count])])

(def boot-repo-cells
  {:repo    boot-repo-repository
   :updated boot-repo-updated
   :stars   boot-repo-stars})

(defn- mount-boot-logo-svg [parent svg-src size]
  (let [el (.querySelector (r/dom-node parent) ".svg")]
    (when el
      (set! (.-innerHTML el) svg-src)
      (let [svg (aget (.getElementsByTagName el "svg") 0)]
        (.setAttribute svg "width" size)
        (.setAttribute svg "height" size)))))

(defn boot-repo [repo-data boot-svg]
  (let [logo-size 90]
    (r/create-class
     {:render
      (fn [_]
        [:div.row-container
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

(defn repo-name-header [repo-name-filter]
  [:th
   "Repositories"
   [:span.repositories-filter
    [:input
     {:type "search" :placeholder "filter by name"
      :on-change (fn [e]
                   (let [string (s/trim (.-value (.-target e)))]
                     (reset! repo-name-filter string)))}]]])

(defn has-substring? [sub target]
  (not (neg? (.indexOf target sub))))

;; (for [col boot-repo-columns]
;;   ^{:key (str "heading" col)}
;;   (if (= col :repo)
;;     [repo-name-header repo-name-filter]
;;     [:th (name col)]))

(defn boot-repos-list [app-state]
  (let [repo-name-filter (r/atom nil)]
    (retrieve-boot-svg app-state)
    (fn [_]
      (let [boot-svg  (get-in @app-state [:data :assets :boot])
            repos     (get-in @app-state [:data :repos])
            repo-info (get-in @app-state [:data :repo-info])]
        [:div.repos.shortstack
         [:div.list-container
          (for [repo repos]
            ^{:key (str repo)}
            [boot-repo
             {:repo repo, :repo-info (get-repo-info repo-info repo)}
             boot-svg])]]))))

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
           ;; submit on enter? or filter while typing?
           ))}]]))

(defn main [app-state]
  [:div.container
   ;;[search app-state]
   [boot-repos-list app-state]])
