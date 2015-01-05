(ns frontend.net
  (:refer-clojure :exclude [get])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [<! chan]]
            [frontend.session :refer [app-state]]
            [cljs-http.client :as xhr]))

(def urls
  {:auth "http://localhost:9090/auth"})

(defn announce! [app-state k data]
  (async/put! (:write-events @app-state) [k data]))

(defn- url-lookup [x]
  (if-not (keyword? x)
    x
    (let [url (cljs.core/get urls x)]
      (assert url "URL keyword wasn't found")
      url)))

(defn request [{:keys [method url data]}]
  (let [resolved-url (url-lookup url)
        methods {:get xhr/get, :post xhr/post}
        request ((cljs.core/get methods method)
                 resolved-url
                 (merge {:with-credentials? false}
                        (when (= :post method)
                          {:edn-params data})))]
    (announce! app-state :request {:method method, :url url, :data data})
    (go (announce! app-state :response (assoc (<! request) :url url)))))

(defn get [url]
  (request {:method :get, :url url}))

(defn post [url data]
  (request {:method :post, :url url, :data data}))
