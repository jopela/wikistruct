(ns wikison.eval-test
  (:require [clojure.test :refer :all]
            [wikison.eval :refer :all]
            [instaparse.core :as insta]
            [clojure.zip :as z]))


(def heading-test-1-in
  (-> [:section [:title "title"] [:text "text"]] z/vector-zip z/down z/right z/down))
  
(def heading-test-1-ex
  :h1)

(deftest heading-test
  (testing "syntax tree should be transformed into clj hash data structure"
    (let [in heading-test-1-in
          ex heading-test-1-ex
          ou (heading in)]
      (is (= ex ou)))))

