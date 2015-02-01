(ns backend.socket
  (:require [clojure.core.async :as async :refer [go-loop <!]]
            [clojure.edn :as edn]
            [org.httpkit.server :as httpkit]
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
         (info "websocket received: %s" topic)
         (case topic
           :init
           (swap! connections update-in [channel] merge data)
           ;; TODO: join client-id to channel in connections atom map
           :cached-repos-data
           (let [{:keys [repos]} data]
             (doseq [repo repos]
               (when repo
                 (async/put! gh-repos-queue repo))))
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

(go-loop []
  (<! data-changed)
  (doseq [[channel _] @connections]
    (httpkit/send! channel (pr-str [:data-changed nil])))
  (recur))

;;
;; Logging
;;
(add-watch connections :info
           (fn [_ _ _ n]
             (info "DEBUG connections: " (pr-str n))
             (info "Current websocket connections: %d" (count n))))
