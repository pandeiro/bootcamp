(ns backend.queues
  (:require [clojure.core.async :as async :refer [chan]]))

(def gh-html-queue (chan))
(def gh-req-queue (chan))
(def gh-repos-queue (chan))
(def gh-repo-info-queue (chan))

;; The data-changed channel is written to every time the repos or
;; repo-info stores change; however it uses a sliding buffer of 1 and
;; is wrapped in a throttled channel so that the :data-changed signal
;; is only dispatched once for any n changes per x time interval.
;;
;; See: backend.socket
(def data-changed (chan (async/sliding-buffer 1)))
