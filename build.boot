(set-env!
 :source-paths   #{"src" "less"}
 :resource-paths #{"html"}
 :dependencies   '[;; boot
                   [adzerk/boot-cljs      "0.0-2629-1" :scope "test"]
                   [adzerk/boot-cljs-repl "0.1.7"      :scope "test"]
                   [adzerk/boot-reload    "0.2.0"      :scope "test"]
                   [adzerk/boot-test      "1.0.3"      :scope "test"]
                   [pandeiro/boot-http    "0.4.2"      :scope "test"]
                   [deraen/boot-less      "0.2.0"      :scope "test"]

                   ;; app
                   [org.clojure/core.async  "0.1.346.0-17112a-alpha"]
                   [com.taoensso/carmine    "2.9.0"]
                   [compojure               "1.3.1"]
                   [ring/ring-defaults      "0.1.3"]
                   [ring/ring-devel         "1.3.2"]
                   [ring-cors               "0.1.6"]
                   [fogus/ring-edn          "0.2.0"]
                   [environ                 "1.0.0"]
                   [http-kit                "2.1.18"]
                   [enlive                  "1.1.5"]
                   [org.webjars/pure        "0.5.0"]
                   [reagent                 "0.5.0-alpha"]
                   [cljs-http               "0.1.24"]
                   [alandipert/storage-atom "1.2.3"]])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload    :refer [reload]]
 '[adzerk.boot-test      :refer [test]]
 '[pandeiro.boot-http    :refer [serve]]
 '[deraen.boot-less      :refer [less]])

(deftask once []
  (comp
   (less)
   (cljs :source-map    true
         :unified-mode  true
         :optimizations :advanced)
   (test :namespaces ['tests.backend])
   (serve :port 8080 :dir "target")
   (serve :port 9090 :handler 'backend/system)))

(deftask dev []
  (comp 
   (watch)
   (speak)
   (reload)
   (cljs-repl)
   (less)
   (cljs :source-map    true
         :unified-mode  true
         :optimizations :advanced)
   (serve :port 8080 :dir "target")
   (serve :port 9090 :handler 'backend/system)))

(deftask tests-watch []
  (comp
   (watch)
   (test :namespaces ['tests.backend])))
