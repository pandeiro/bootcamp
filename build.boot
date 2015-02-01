(set-env!
 :source-paths   #{"src" "less" "test"}
 :resource-paths #{"html" "conf" "data"}
 :dependencies   '[;; boot
                   [adzerk/boot-cljs          "0.0-2727-0" :scope "test"]
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
                   [http-kit                "2.1.18"]
                   [enlive                  "1.1.5"]
                   [org.webjars/pure        "0.5.0"]
                   [cljsjs/react            "0.12.2-5"]
                   [cljsjs/moment           "2.6.0-3"]
                   [reagent                 "0.5.0-alpha"]
                   [shodan                  "0.4.1"]
                   [cljs-http               "0.1.24"]
                   [alandipert/storage-atom "1.2.3"]])

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
  [f function SYM  sym   "The function to run."
   a args     ARGS [sym] "Any args to pass"]
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

(deftask serve-backend
  [d dev bool "Run in dev-mode with reloading of Clojure files"]
  (serve :port    9090
         :httpkit true
         :init    'backend.services.github/start-worker
         :cleanup 'backend.services.github/stop-worker
         :handler (if dev
                    'backend/system-dev
                    'backend/system)))

(deftask compile-frontend []
  (comp
   (less)
   (cljs :optimizations :none, :source-map true)))

(deftask serve-frontend []
  (serve :port 8080, :dir "target"))

(deftask dev-cljs []
  (comp
   (cljs-repl)
   (cljs :optimizations :none, :source-map true)))

(deftask dev []
  (comp
   (serve-backend)
   (watch)
   (speak)
   (reload :on-jsload 'frontend.app/init)
   (less)
   (dev-cljs)
   (serve-frontend)))

(deftask ci []
  (comp
   (serve-backend)
   (compile-frontend)))
