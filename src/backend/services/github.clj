(ns backend.services.github
  (:require
   [clojure.string :as s]
   [org.httpkit.client :as http]
   [net.cgrand.enlive-html :as enlive]
   [taoensso.carmine :as car]
   [backend.stores :refer [with-redis]]))

;;; Util

(defn- parse-int [x]
  (try (Integer/parseInt x)
       (catch Exception e nil)))

(defn enliven [html search-path]
  (enlive/select (enlive/html-snippet html)
    (if (vector? search-path)
      search-path
      (vector search-path))))

(def apply-conj (partial apply conj))

;;; Network

(def search-url
  (str "https://github.com/search?"
       "utf8=%E2%9C%93&"
       "q=deftask+set-env%21+extension%3Aboot&"
       "type=Code&"
       "ref=searchresults"))

(def ua
  (str "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
       "(KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36"))

(defn search-github []
  (http/request {:url search-url :user-agent ua}))

(defn search-github-page [n]
  (http/request {:url (str search-url "&p=" n) :user-agent ua
                 :headers {"Referer" search-url}}))

;;; Processing

(defn count-boot-repos [html]
  (let [text (-> html
               (enliven :h3)
               first
               enlive/text)]
    (parse-int (re-find #"[0-9]+" text))))

(defn count-boot-repo-pages [html]
  (let [selection (enliven html [:.pagination :a])]
    (apply max
           (->> selection
             (map enlive/text)
             (map parse-int)
             (remove nil?)))))

(defn parse-repo [user-and-repo]
  (let [[user repo] (s/split user-and-repo #"/")]
    {:user user, :repo repo}))

(defn get-boot-repos [html]
  (->> (enliven html [:.code-list :p.title :a])
    (map enlive/text)
    (partition 2)
    (map first)
    (map parse-repo)))

;;; Main

(defn get-all-boot-repos []
  (let [first-page @(search-github)]
    (let [html  (:body first-page)
          pages (map search-github-page
                     (range 2 (inc (count-boot-repo-pages html))))
          results (atom [])]
      (swap! results conj (get-boot-repos html)) ; first page
      (doseq [page pages]
        (Thread/sleep 60000)
        (let [{html :body status :status} @page]
          (if (= 200 status)
            (swap! results conj (get-boot-repos html))
            (swap! results conj {:status status}))))
      @results)))

(defn add-github-user [user]
  (with-redis
    (car/sadd :boot-users user)))

