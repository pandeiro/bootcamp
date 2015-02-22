(ns frontend.util.helpers)

(defn once [f & args]
  (let [already? (atom nil)]
    (when-not @already?
      (reset! already? true)
      (apply f args))))

(defn compare-iso-dates [a b]
  (assert (and (string? a) (string? b)) "compare-iso-dates expects strings")
  (let [date-a (js/Date. a)
        date-b (js/Date. b)]
    (cond (< a b) -1
          (> a b) 1
          (= a b) 0)))

(defn new-client-id []
  (str
   (.getTime (js/Date.)) "-"
   (apply str (repeatedly 10 #(first (shuffle (range 10)))))))
