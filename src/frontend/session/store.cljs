(ns frontend.session.store
  (:require
   [alandipert.storage-atom :refer [local-storage]]))

(def store (local-storage (atom {}) :app))

;;; Watcher

(defn watch-and-store [state-atom path]
  (add-watch state-atom (keyword (gensym))
             (fn [_ _ old new]
               (when-not (= (get-in old path)
                            (get-in new path))
                 (swap! store assoc-in path (get-in new path)))))
  state-atom)
