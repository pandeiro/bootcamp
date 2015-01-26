(ns backend.queues
  (:require [clojure.core.async :refer [chan]]))

(def gh-html-queue (chan))
(def gh-req-queue (chan))
(def gh-repos-queue (chan))
(def gh-repo-info-queue (chan))

(def data-changed (chan))
