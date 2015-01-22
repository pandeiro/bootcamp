(ns backend.util.validation)

;;
;; Just stubs for now, but eventually validation functions
;; should take a single map with all the data they need and
;; return a map with either :ok true or :err and a vector of
;; error messages.
;;

(defn validate-user [{:keys [email password]}]
  {:ok true})
