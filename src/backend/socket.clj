(ns backend.socket
  (:require [clojure.core.async :as async :refer [go-loop <!]]
            [clojure.edn :as edn]
            [org.httpkit.server :as httpkit]
            [backend.logging :refer [info warn]]
            [backend.queues :refer [gh-repos-queue
                                    gh-repo-info-queue
                                    data-changed]]))

(def connections
  (atom {}))

(defn socket-handler [req]
  (httpkit/with-channel req channel

    (swap! connections assoc channel req)

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
           :cached-repos-data
           (let [{:keys [repos]} data]
             (doseq [repo repos]
               (when repo
                 (async/put! gh-repos-queue repo))))
           :cached-repo-info-data
           (let [[repo repo-info] data]
             (when (and repo repo-info)
               (async/put! gh-repo-info-queue repo-info)))
           nil)
         (info "Received socket data: %d repos and %d repo details"
               (count (:repos data))
               (count (:repo-info data)))
         (httpkit/send! channel (pr-str {:message "Thank you!"})))))))

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
             (info "Current websocket connections: %d" (count n))))
