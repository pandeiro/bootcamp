(ns backend.tests
  (:require
   [clojure.test :refer :all]
   [backend.services.github :as gh]))

(deftest addition
  (testing "basic math"
    (is (= 6 (+ 1 2 3)))))

(deftest merge-github-info-if-newer
  (let [repo-info-store
        {{:repo "boot-show-u", :user "whodidthis"}
         {:updated_at "2014-11-20T13:34:09Z"}}]
    (testing "merge-if-newer for merging repo info sent from client"
      (let [earlier-version [{:repo "boot-show-u", :user "whodidthis"}
                             {:updated_at "2014-11-12T13:34:09Z"}]]
        (is (= (gh/merge-if-newer repo-info-store earlier-version)
               repo-info-store))))))
