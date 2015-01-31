(ns frontend.views
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [chan <!]]
            [goog.string.format]
            [clojure.string :as s]
            [reagent.core :as r]
            [cljs-http.client :as http]
            [shodan.console :as console :include-macros true]
            [shodan.inspection :refer [inspect]]
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
  (when x
    (.fromNow (js/moment x))))

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
  [:div.row-container
   {:on-double-click
    (fn [_]
      (if (not-empty (:repo-info repo-data))
        (inspect repo-data)
        (session/put-event! [:repo-info-request (:repo repo-data)])))}
   (for [col boot-repo-columns]
     (let [boot-repo-cell (get boot-repo-cells col)]
       ^{:key (str (get-in repo-data [:repo :user])
                   (get-in repo-data [:repo :repo])
                   col)}
       [boot-repo-cell repo-data]))])

(defn- get-repo-info [repo-info repo]
  (get repo-info repo))

(defn has-substring? [sub target]
  (not (neg? (.indexOf target sub))))

;; (for [col boot-repo-columns]
;;   ^{:key (str "heading" col)}
;;   (if (= col :repo)
;;     [repo-name-header repo-name-filter]
;;     [:th (name col)]))

(defn updated-at-sorter [[_ {:keys [updated_at]}]]
  (or updated_at -1))

(def boot-repos-sorters
  {:updated_at updated-at-sorter})

(defn boot-repos-list [app-state]
  (let [repo-name-filter (r/atom nil)
        repo-sort-key    (r/atom :updated_at)
        repo-sort-order  (r/atom :desc)]
    (retrieve-boot-svg app-state)
    (fn [_]
      (console/log "boot-repos-list render")
      (let [boot-svg  (get-in @app-state [:data :assets :boot])
            repos     (get-in @app-state [:data :repos])
            repo-info (get-in @app-state [:data :repo-info])
            unified   (into {} (map #(vector % (get repo-info %)) repos))
            displayed (if (not-empty @repo-name-filter)
                        (filter #(has-substring? @repo-name-filter (:repo (first %))) unified)
                        unified)
            sorted    (sort-by (boot-repos-sorters @repo-sort-key)
                               displayed)
            ordered   (if (= :desc @repo-sort-order)
                        (reverse sorted)
                        sorted)]
        [:div.repos.shortstack
         [:div {:style {:display "flex"
                        :flex-direction "row"
                        :justify-content "space-between"
                        :padding "0.5em 1em"}}
          [:input {:type "text" :placeholder "filter by name"
                   :style {:border "none"
                           :background "none"
                           :color "#ccc"
                           :font-style "italic"}
                   :on-change #(reset! repo-name-filter (s/trim (.-value (.-target %))))}]
          [:p {:style {:margin 0
                       :color "#985"}}
           (str "Showing " (count displayed) " repositories " )]]
         [:div.list-container
          (for [[k v] ordered]
            ^{:key (str k)}
            [boot-repo {:repo k, :repo-info v} boot-svg])]]))))

(defn main [app-state]
  [:div.container
   ;;[search app-state]
   [boot-repos-list app-state]])
