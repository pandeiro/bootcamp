(ns tests.backend
  (:require
   [clojure.test :refer :all]))

(deftest addition
  (testing "basic math"
    (is (= 6 (+ 1 2 3)))))
