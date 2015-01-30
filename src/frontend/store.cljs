(ns frontend.store
  (:require
   [alandipert.storage-atom :refer [local-storage]]))

(def store (local-storage (atom {}) :app))

;;; Debugging

(def debug? (atom false))

(defn toggle-debug []
  (.log js/console "local-store debugging:" (swap! debug? not)))

(add-watch store :debug (fn [k r o n]
                          (when @debug?
                            (.log js/console "local-storage atom:" (pr-str n)))))

;;; Watcher

(defn watch-and-store [state-atom path]
  (add-watch state-atom (keyword (gensym))
             (fn [_ _ old new]
               (when-not (= (get-in old path)
                            (get-in new path))
                 (swap! store assoc-in path (get-in new path)))))
  state-atom)