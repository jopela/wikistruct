(ns wikison.filters-test
  (:require [clojure.test :refer :all]
            [wikison.filters :refer :all]
            [clojure.zip :as zip]))

;(deftest subsection?-1
;  (testing "a section with no subsection should return false"
;    (let [in subsection?-1-in
;          ex false
;          ou (


(def del-sec-with-title-1-in
  [:article [:abstract "abstract"]
   [:sections 
    [:section [:title "title"] [:text "text"]]]])

(def del-sec-with-title-1-ex
  [:article [:abstract "abstract"]])

(deftest del-sec-with-title-1
  (testing "tree containing only a section with a title that must be removed must not contain section"
    (let [in del-sec-with-title-1-in
          ex del-sec-with-title-1-ex
          ou (del-sec-with-title in #{"BAD"})]
      (is (= ex ou)))))

; delete empty section test.
(def del-empty-sec-1-in
  [:article
   [:sections
    [:section [:title "title"] [:text "test"]]
    [:sections
     [:section [:title "title1"] [:text "\n\n"]]]]])
  
(def del-empty-sec-1-ex
  [:article
   [:sections
    [:section [:title "title"] [:text "test"]]]])

(deftest del-empty-sec-1
  (testing "section with blank text and no children should be removed."
    (let [in del-empty-sec-1-in
          ex del-empty-sec-1-ex
          ou (del-empty-sec in)]
      (is (= ex in)))))
          
          
