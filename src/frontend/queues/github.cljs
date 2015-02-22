(ns frontend.queues.github
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as async :refer [chan <!]]
            [clojure.set :as set]
            [shodan.console :as console :include-macros true]
            [frontend.session.state :as session]))

(defn repo-info-worker
  "Polls the GitHub API for repository information about 5 repos after
  every ten minutes, preferentially repos without existing information
  in the app-state store (otherwise chosen randomly)"
  [app-state]
  (go-loop []
    (let [repos  (get-in @app-state [:data :repos])
          repo-info-repos (get-in @app-state [:data :repo-info])
          missing (set/difference repos (set (keys repo-info-repos)))
          chosen (take 5 (shuffle missing))]
      (<! (async/timeout (* 1000 60 5)))
      (doseq [repo (if (= 5 (count chosen))
                     chosen
                     (take (- 5 (count chosen)) (shuffle repos)))]
        (session/put-event! [:repo-info-request repo]))
      (recur))))
