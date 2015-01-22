(ns frontend.store
  (:require
   [alandipert.storage-atom :refer [local-storage]]))

(def store (local-storage (atom {}) :app))

(add-watch store :debug (fn [k r o n]
                          (.log js/console "local-storage atom:" (pr-str n))))

(defn watch-and-store [state-atom path]
  (add-watch state-atom (keyword (gensym))
             (fn [_ _ old new]
               (when-not (= (get-in old path)
                            (get-in new path))
                 (swap! store assoc-in path (get-in new path)))))
  state-atom)
