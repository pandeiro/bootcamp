(ns frontend.views.list
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   cljsjs.moment
   [cljs.core.async :as async :refer [chan <!]]
   [goog.string.format]
   [clojure.string :as s]
   [reagent.core :as r]
   [cljs-http.client :as http]
   [shodan.inspection :refer [inspect]]
   [frontend.session.state :as session]))

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
  [:repo :updated])

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
     {:style {:width "270px"}}
     [:a {:href (format "https://github.com/%s/%s" user repo)
          :target "_blank"}
      repo]]))

(defn boot-repo-refresh [{:keys [repo]}]
  [:div
   [:button
    {:style {:background "rgba(255,255,255,0.3)" :border-radius "2px"
             :height "25px" :border "none" :color "#551"}
     :on-click #(session/put-event! [:repo-info-request repo])}
    "↺"]])

(defn boot-repo-stars [data]
  (let [stars (get-in data [:repo-info :stargazers_count])]
    [:div {:style {:font-size "14px" :color "#532"}}
     (when stars (str "★ " stars))]))

(defn boot-repo-updated [data]
  [:div.repo-updated {:style {:width "160px" :font-style "italic" :color "#542"
                              :font-size "14px"}}
   (rel-time (get-in data [:repo-info :updated_at]))])

(defn boot-repo-row [data]
  [:div.row-item.repo
   [boot-repo-refresh data]
   [boot-repo-user data]
   [boot-repo-name data]
   [boot-repo-updated data]
   [boot-repo-stars data]])

(defn boot-repo [repo-data]
  [:div.row-container
   {:on-double-click
    (fn [_]
      (when (not-empty (:repo-info repo-data))
        (inspect repo-data)))}
   [boot-repo-row repo-data]])

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
  (or updated_at ""))

(defn stars-sorter [[_ {:keys [stargazers_count]}]]
  (or stargazers_count 0))

(defn repo-name-sorter [[{:keys [repo]} _]]
  repo)

(def boot-repos-sorters
  {:updated   updated-at-sorter
   :stars     stars-sorter
   :repo-name repo-name-sorter})

(defn boot-repos-sortby [repo-sort-key]
  [:div.sort
   [:span
    "Sort by: "]
   [:select
    {:on-change #(reset! repo-sort-key (keyword (.-value (.-target %))))}
    [:option {:value "updated"} "updated"]
    [:option {:value "stars"} "stars"]
    [:option {:value "repo-name"} "repo name"]]])

(defn- trimmed-val [e]
  (s/trim (.-value (.-target e))))

(defn boot-repos-filterby [repo-name-filter]
  [:div.filter
   [:span
    "Filter by name: "]
   [:input {:type "text" :placeholder "eg, 'cljs'"
            :style {:outline "none"
                    :padding "4px"}
            :on-change #(reset! repo-name-filter (trimmed-val %))}]])

(defn boot-repos-list [app-state]
  (let [repo-name-filter (r/atom nil)
        repo-sort-key    (r/atom :updated)
        repo-sort-order  (r/atom :desc)]
    (fn [_]
      (let [{:keys [repos repo-info]} (get-in @app-state [:data])]
        (let [unified   (into {} (map #(vector % (get repo-info %)) repos))
              displayed (if (not-empty @repo-name-filter)
                          (filter #(has-substring? @repo-name-filter (:repo (first %))) unified)
                          unified)
              sorted    (sort-by (boot-repos-sorters @repo-sort-key)
                                 displayed)
              ordered   (if (= :desc @repo-sort-order)
                          (reverse sorted)
                          sorted)]
          [:div.repos.shortstack
           [:div.controls
            {:style {:display "flex"
                     :flex-direction "row"
                     :justify-content "space-between"
                     :padding "0.5em 1em"}}
            [boot-repos-sortby repo-sort-key]
            [boot-repos-filterby repo-name-filter]]
           [:div.list-container
            (for [[k v] ordered]
              ^{:key (str k)}
              [boot-repo {:repo k, :repo-info v}])]])))))

