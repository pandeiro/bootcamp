(ns tests.backend
  (:require
   [taoensso.carmine :as car]
   [backend.stores :refer [with-redis]]
   [clojure.test :refer :all]))

(deftest redis-present
  (testing "whether redis can be connected to"
    (is (= "PONG" (with-redis (car/ping))))))

(deftest addition
  (testing "basic math"
    (is (= 6 (+ 1 2 3)))))
