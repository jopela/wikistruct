(ns wikison.eval-test
  (:require [clojure.test :refer :all]
            [wikison.parse :refer :all]
            [instaparse.core :as insta]))


(deftest tree-eval-clj
  (testing "syntax tree should be transformed into clj hash data structure"
    (is (= 1 2))))



