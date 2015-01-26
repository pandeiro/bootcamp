(ns frontend.github
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as async :refer [chan <!]]
            [clojure.set :as set]
            [frontend.session :as session]))

(defn repo-info-worker [app-state]
  (go-loop []
    (let [repos  (get-in @app-state [:data :repos])
          repo-info-repos (get-in @app-state [:data :repo-info])
          missing (set/difference repos (set (keys repo-info-repos)))
          chosen (take 5 (shuffle missing))]
      (doseq [repo (if (pos? (count chosen))
                     chosen
                     (take 5 (shuffle repos)))]
        (session/put-event! [:repo-info-request repo]))
      (<! (async/timeout (* 1000 60 5)))
      (recur))))
