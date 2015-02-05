(ns backend.queues
  (:require [clojure.core.async :as async :refer [chan]]))

(def gh-html-queue (chan))
(def gh-req-queue (chan))
(def gh-repos-queue (chan))
(def gh-repo-info-queue (chan))

;; The data-changed channel is written to every time the repos or
;; repo-info stores change; it is wrapped in a throttled channel before
;; those changes get dispatched as websocket messages to the client.
;;
;; See: backend.socket
(def data-changed (chan (async/dropping-buffer 1)))
