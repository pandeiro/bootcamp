(ns backend.api
  (:require [compojure.core :refer [defroutes GET POST DELETE]]
            ))

(def handler
  (fn [req]
    {:status  200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body    "{\"ok\": true}"}))

(defroutes routes
  (GET "/" []
    "Sanity check")
  (POST "/user" [username password]
    )
  (POST "/auth" [username password]
    )
  (DELETE "/auth" []
    ))
