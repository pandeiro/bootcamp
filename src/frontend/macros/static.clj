(ns frontend.macros.static
  (:require
   [environ.core :refer [env]]
   [clojure.edn :as edn]
   [clojure.walk :refer [postwalk]]
   [clojure.java.io :as io]))

(def static-config "static.edn")

(defn transform-map
  "Given a key of either :dev or :prod, walks a map
  replacing any keys whose values are a map with keys
  :dev and :prod with the value of the given key."
  [k config-map]
  (let [replace-fn (fn [x]
                     (if (and (map? x)
                              (contains? x :dev)
                              (contains? x :prod))
                       (get x k)
                       x))]
    (postwalk replace-fn config-map)))

(defmacro read-config
  "Allows for a static configuration file with, eg
  different URLs for use with development and production
  environment services.

  Filters according to either PRODUCTION or PROD being non-nil.

  Otherwise uses development (:dev) values."
  []
  (let [cfg-file (io/resource static-config)]
    (assert cfg-file (str "Static config file not found on resource path: "
                          static-config))
    (let [config (if cfg-file
                   (edn/read-string (slurp cfg-file))
                   {})]
        (if (or (env :production) (env :prod))
          (transform-map :prod config)
          (transform-map :dev config)))))


