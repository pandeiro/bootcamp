(ns backend.socket
  (:require [clojure.core.async :as async :refer [go-loop <!]]
            [clojure.edn :as edn]
            [org.httpkit.server :as httpkit]
            [throttler.core :refer [throttle-chan]]
            [backend.logging :refer [info warn]]
            [backend.queues :refer [gh-repos-queue
                                    gh-repo-info-queue
                                    data-changed]]))

(defonce connections
  (atom {}))

(defn socket-handler [req]
  (httpkit/with-channel req channel

    (swap! connections assoc channel {:request req})

    (httpkit/on-close
     channel
     (fn [status]
       (swap! connections dissoc channel)
       (info "Socket closed: %s" status)))

    (httpkit/on-receive
     channel
     (fn [raw]
       (let [[topic data] (edn/read-string raw)]
         (case topic

           ;; Add :client-id to channel map in connections (as long as
           ;; there isn't an existing client-id for that channel)
           :init
           (do
             (info "initial websocket connection from %s" (pr-str data))
             (swap! connections update-in [channel]
                    (fn [{:keys [client-id] :as conn}]
                      (if-not client-id
                        (merge conn data)
                        conn))))

           ;; Iterate through repos set and put each on queue
           :cached-repos-data
           (let [{:keys [repos]} data]
             (doseq [repo repos]
               (when repo
                 (async/put! gh-repos-queue repo))))

           ;; 
           :cached-repo-info-data
           (when (not-empty data)
             (async/put! gh-repo-info-queue data))
           nil)
         ;; example: echo
         ;;
         ;; (->> {:message "Thanks!"}
         ;;   pr-str
         ;;   (httpkit/send! channel))
         )))))


(defn start-notification-worker []
  (def notification-worker
    (future
      (let [data-changed* (throttle-chan data-changed 6 :hour)]
        (go-loop []
          (<! data-changed*)
          (info "Data changed, broadcasting to sockets")
          (doseq [[channel _] @connections]
            (httpkit/send! channel (pr-str [:data-changed nil])))
          (recur))))))

(defn stop-notification-worker []
  (future-cancel notification-worker))

;;
;; Logging
;;
(add-watch connections :info
           (fn [_ _ _ n]
             (info "DEBUG connections: " (pr-str n))
             (info "Current websocket connections: %d" (count n))))
