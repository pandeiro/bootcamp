(ns backend.services.minitwitter
  (:require
   [backend.stores :refer [with-redis]]
   [backend.util :refer [random-str now]]
   [taoensso.carmine :as car]))

;;;
;;; Data layout: http://redis.io/topics/twitter-clone
;;;

;;; Followers

(defn follow [follower-id user-id]
  (with-redis
    (car/zadd (str "followers:" user-id) (now) follower-id)
    (car/zadd (str "following:" follower-id) (now) user-id)))

;;; Registering updates

(defn post [user-id post-id]
  (with-redis
    (car/lpush (str "posts:" user-id) post-id)))

;;; Creating updates

(defn next-post-id []
  (with-redis (car/incr :next_post_id)))

(defn followers-for [user-id]
  (with-redis
    (car/zrange (str "followers:" user-id) 0 -1)))

(defn record-post [user-id post-id ts content]
  (with-redis
    (car/hmset* (str "post:" post-id)
                {:user_id user-id
                 :time    ts
                 :body    content})))

(defn broadcast-post [user-id post-id]
  (doseq [follower-id (followers-for user-id)]
    (post follower-id post-id)))

(defn update-timeline [post-id]
  (with-redis
    (car/lpush :timeline post-id)
    (car/ltrim :timeline 0 1000)))

(defn create-post [user-id content]
  (let [post-id (next-post-id)]
    (record-post user-id post-id (now) content)
    (broadcast-post user-id post-id)
    (update-timeline post-id)))

;;; Retrieving updates w/ pagination

(defn paginate-posts [user-id start how-many]
  (let [redis-key (if user-id (str "posts:" user-id) :timeline)]
    (with-redis
      (car/lrange redis-key start (+ start how-many)))))
