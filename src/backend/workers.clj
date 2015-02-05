(ns backend.workers
  (:require
   [backend.services.github :as gh]
   [backend.socket :as ws]))

(defn start-workers []
  (gh/start-worker)
  (ws/start-notification-worker))

(defn stop-workers []
  (gh/stop-worker)
  (ws/stop-notification-worker))
