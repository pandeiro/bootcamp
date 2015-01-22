(ns backend.util.response)

(defn response [status & [data]]
  {:status  status
   :headers (if data
              {"Content-Type" "application/edn"}
              {})
   :body    (if data (pr-str data))})

(defn created [& [data]]
  (response 201 data))

(defn ok [& [data]]
  (response 200 data))

(defn unauthorized []
  (response 403))
