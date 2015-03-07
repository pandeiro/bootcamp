(set-env!
 :source-paths   #{"src" "less" "test"}
 :resource-paths #{"html" "conf"}
 :dependencies   '[;; boot
                   [adzerk/boot-cljs          "0.0-2760-0"     :scope "test"]
                   [pandeiro/boot-http        "0.6.3-SNAPSHOT" :scope "test"]
                   [deraen/boot-less          "0.2.0"          :scope "test"]
                   [adzerk/boot-reload        "0.2.4"          :scope "test"]
                   [adzerk/boot-test          "1.0.2"          :scope "test"]
                   [adzerk/boot-cljs-repl     "0.1.9"          :scope "test"]

                   ;; override included versions
                   [org.clojure/clojurescript "0.0-2913"       :scope "test"]
                   [org.clojure/tools.nrepl   "0.2.7"          :scope "test"]

                   ;; app
                   [org.clojure/core.async   "0.1.346.0-17112a-alpha"]
                   [compojure                "1.3.1"]
                   [ring/ring-defaults       "0.1.3"]
                   [ring/ring-devel          "1.3.2"]
                   [ring-cors                "0.1.6"]
                   [fogus/ring-edn           "0.2.0"]
                   [environ                  "1.0.0"]
                   [pandeiro/http-kit        "2.1.20-SNAPSHOT"]
                   [enlive                   "1.1.5"]
                   [org.webjars/pure         "0.5.0"]
                   [cljsjs/react-with-addons "0.12.2-7"]
                   [cljsjs/moment            "2.9.0-0"]
                   [reagent                  "0.5.0-alpha3"
                    :exclusions
                    [cljsjs/react]]
                   [shodan                   "0.4.1"]
                   [cljs-http                "0.1.24"
                    :exclusions
                    [com.cemerick/austin]]
                   [alandipert/storage-atom  "1.2.3"]
                   [org.clojure/core.cache   "0.6.4"]
                   [clj-webdriver            "0.6.1"]
                   [throttler                "1.0.0"]])

(require
 '[boot.util             :as util]
 '[boot.pod              :as pod]
 '[clojure.java.io       :as io]
 '[adzerk.boot-cljs      :refer [cljs]]
 '[pandeiro.boot-http    :refer [serve]]
 '[deraen.boot-less      :refer [less]]
 '[adzerk.boot-reload    :refer [reload]]
 '[adzerk.boot-test      :refer [test]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]])

;;
;; Backend
;;

(deftask serve-backend
  [d dev bool "Run in dev-mode with reloading of Clojure files"]
  (serve :port    9090
         :httpkit true
         :init    'backend.workers/start-workers
         :cleanup 'backend.workers/stop-workers
         :handler (if dev
                    'backend/system-dev
                    'backend/system)))

;;
;; Frontend
;;

(deftask compile-frontend []
  (comp
   (less)
   (cljs :optimizations :advanced, :source-map true)))

(deftask serve-frontend []
  (serve :port 8484, :dir "target"))

(deftask dev-cljs []
  (comp
   (cljs-repl)
   (cljs :optimizations :none, :source-map true)))

(deftask run-frontend []
  (comp
   (watch)
   (speak)
   (reload :on-jsload 'frontend.app/init)
   (less)
   (dev-cljs)
   (serve-frontend)))

;;
;; Both (useful for development and CI)
;;

(deftask dev []
  (comp
   (serve-backend :dev true)
   (watch)
   (speak)
   (run-frontend)))

(deftask ci []
  (comp
   (serve-backend)
   (compile-frontend)))

