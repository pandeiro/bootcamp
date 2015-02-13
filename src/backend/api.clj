(ns backend.api
  (:require [compojure.core :refer [defroutes GET POST DELETE context]]

            [backend.util.validation :as v]
            [backend.util.response :as resp]
            [backend.socket :as ws]
            [backend.stores :as stores]))

;; HTTP Kit example

;; (defn handler [request]
;;   (with-channel request channel
;;     (on-close channel (fn [status] (println "channel closed: " status)))
;;     (on-receive channel (fn [data] ;; echo it back
;;                           (send! channel data)))))


(defroutes routes

  (GET "/ws" []
    ws/socket-handler)

  (context "/api" []

    (GET "/repos" []
      (resp/ok {:ok        true
                :repos     @stores/repos
                :repo-info @stores/repo-info}))

    (GET "/stats" []
      (resp/ok {:ok    true
                :stats @stores/stats}))

    (GET "/conn" []
      (resp/ok {:ok   true
                :conn @ws/connections}))))
