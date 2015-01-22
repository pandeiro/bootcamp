(ns backend.util)

(defn random-str []
  (.toString (java.util.UUID/randomUUID)))

(defn now []
  (System/currentTimeMillis))

