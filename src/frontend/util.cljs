(ns frontend.util)

(defn once [f & args]
  (let [already? (atom nil)]
    (when-not @already?
      (reset! already? true)
      (apply f args))))
