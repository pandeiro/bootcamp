(ns frontend.socket
  (:require [cljs.core.async :as async]
            [cljs.reader :refer [read-string]]))

(defn connect [app-state]
  (let [ws-url (get-in @app-state [:config :urls :ws])
        socket (js/WebSocket. ws-url)]

    ;; TODO: transform this into a processing queue similar
    ;; to the server-side
    (defn send! [data]
      (.send socket (pr-str data)))

    ;; Send locally cached data to server-side stores
    (set! (.-onopen socket)
          (fn [_]
            (let [data (:data @app-state)]
              (send! [:cached-repos-data (:repos data)])
              ;; send kv-by-kv b/c of httpkit ws limitations
              (doseq [repo-info (:repo-info data)]
                (.log js/console "debug send repo info")
                (let [[k v] repo-info]
                  (.log js/console (pr-str [:cached-repo-info-data [k (select-keys v [:updated_at])]])))
                (send! [:cached-repo-info-data repo-info])))))
    
    ;; Turn everything over to the event-loop
    (set! (.-onmessage socket)
          (fn [e]
            (let [data (read-string (.-data e))]
              (async/put! (:write-events @app-state) [:message data]))))
    ))
