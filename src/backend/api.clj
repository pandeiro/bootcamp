(ns backend.api
  (:require [compojure.core :refer [defroutes GET POST DELETE]]
            [backend.services.users :as users]
            [backend.services.minitwitter :as tw]
            [backend.util.validation :as v]
            [backend.util.response :as resp]))

(def handler
  (fn [req]
    {:status  200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body    "{\"ok\": true}"}))

(defn- authorized? [req]
  (= (get-in req [:params :user-id])
     (get-in req [:session :user-id])))

(defroutes routes
  (GET "/debug" req
    (pr-str req))

  ;;; Users

  (POST "/user" [email password]
    (let [{:keys [ok err]} (v/validate-user {:email email, :password password})]
      (when ok
        (users/create-user email password)
        (resp/created))))

  (POST "/auth" [email password]
    (when-let [{:keys [user-id auth]} (users/authenticate email password)]
      (-> (resp/ok {:username email, :user-id user-id})
        (assoc :session {:user-id user-id, :auth auth}))))

  (DELETE "/auth" req
    (when (authorized? req)
      (let [user-id (get-in req [:params :user-id])]
        (users/logout user-id)
        (-> (resp/ok)
          (assoc :session {})))))

  ;;; Updates

  (GET "/updates" {{:keys [user-id start limit]} :params session :session :as req}
    (let [start (or start 0)
          limit (or limit 25)
          user-id (when (and user-id (authorized? req)) user-id)]
      (resp/ok (tw/paginate-posts user-id start limit))))

  (POST "/updates" req
    (if (authorized? req)
      (let [{:keys [user-id content]} (get-in req [:params])]
        (tw/create-post user-id content)
        (resp/created))
      (resp/unauthorized)))

  ;;; Following

  (POST "/follow" req
    (when (authorized? req)
      (let [{:keys [user-id follower-id]} (get-in req [:params])]
        (tw/follow follower-id user-id)
        (resp/created))))

  )
