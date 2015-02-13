(ns backend.util
  (:import [java.time LocalDateTime LocalDate]
           [java.time.format DateTimeFormatter]
           [java.util Date]))

(defn random-str []
  (.toString (java.util.UUID/randomUUID)))

(defn now []
  (System/currentTimeMillis))

(defn read-iso-date [string]
  (LocalDateTime/parse string DateTimeFormatter/ISO_DATE_TIME))

(defn compare-iso-dates
  "Takes two ISO_DATE_TIME strings and returns -1 if the first is earlier,
  0 if the two are equal, and 1 if the first is later."
  [string1 string2]
  (.compareTo (read-iso-date string1)
              (read-iso-date string2)))

(defn now-as-date-string []
  (.toString (LocalDate/now)))
