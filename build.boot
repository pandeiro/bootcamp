(set-env!
 :source-paths   #{"src" "less" "test"}
 :resource-paths #{"html" "conf"}
 :dependencies   '[;; boot
                   [adzerk/boot-cljs          "0.0-2760-0" :scope "test"]
                   [pandeiro/boot-http        "0.6.1"      :scope "test"]
                   [deraen/boot-less          "0.2.0"      :scope "test"]
                   [adzerk/boot-reload        "0.2.4"      :scope "test"]
                   [adzerk/boot-test          "1.0.2"      :scope "test"]
                   [adzerk/boot-cljs-repl     "0.1.7"      :scope "test"]

                   ;; app
                   [org.clojure/core.async  "0.1.346.0-17112a-alpha"]
                   [compojure               "1.3.1"]
                   [ring/ring-defaults      "0.1.3"]
                   [ring/ring-devel         "1.3.2"]
                   [ring-cors               "0.1.6"]
                   [fogus/ring-edn          "0.2.0"]
                   [environ                 "1.0.0"]
                   [http-kit                "2.1.19"]
                   [enlive                  "1.1.5"]
                   [org.webjars/pure        "0.5.0"]
                   [cljsjs/react            "0.12.2-5"]
                   [cljsjs/moment           "2.9.0-0"]
                   [reagent                 "0.5.0-alpha"]
                   [shodan                  "0.4.1"]
                   [cljs-http               "0.1.24"]
                   [alandipert/storage-atom "1.2.3"]
                   [org.clojure/core.cache  "0.6.4"]
                   [clj-webdriver           "0.6.1"]
                   [throttler               "1.0.0"]])

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

(deftask run-once
  "Run a function of no args just one time in a pipeline"
  [f function SYM  sym   "The function to run."]
  (let [worker (pod/make-pod (get-env))
        start  (delay (pod/with-eval-in worker
                        (require (symbol (namespace '~function)) :reload)
                        (def instance (future ((resolve '~function)))))
                      (util/info "<< Running %s once... >>\n" (str function)))]
    (cleanup
     (util/info "<< Stopping instance of %s... >>\n" (str function))
     (pod/with-eval-in worker
       (future-cancel instance)))
    (with-pre-wrap fileset
      @start
      fileset)))

;; WIP
;; (deftask release []
;;   (comp
;;    (less)
;;    (add-react {:min? true})
;;    (cljs :source-map    true
;;          :optimizations :advanced)))

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
  (cljs :optimizations :none, :source-map true))

(deftask run-frontend []
  (comp
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
