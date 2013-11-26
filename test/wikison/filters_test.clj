(ns wikison.filters-test
  (:require [clojure.test :refer :all]
            [wikison.filters :refer :all]))

; pre-process transform tests
(deftest merge-sentence-test
  (testing "tree containing sentences must be merge into text"
    (let [in [:text [:sentence "qui seme le vent...\n"] 
             [:sentence "recolte la tempete ...\n"]]
          ex [:text "qui seme le vent...\nrecolte la tempete ...\n"]
          ou (merge-sentence in)]
      (is (= ex ou)))))

;helper function tests
(deftest has-child?-test-no
  (testing "section with no subsection have no child"
    (let [in [[:text "some text"] [:title "no child section"]] 
          ex nil
          ou (has-child? in)]
      (is (= ex ou)))))

(deftest has-child?-test-yes
  (testing "section with subsection have childs"
    (let [in [[:text "some text"] [:title "no child section"] 
              [:subs1 [:sub1 [:title "section"] [:text "some"]]]] 
          ex true
          ou (has-child? in)]
      (is (= ex ou)))))

(deftest text-val-test-yes
  (testing "section element with a text value"
    (let [in [[:title "title"] [:some-tag "tag"] [:text "text"]]
          ex "text"
          ou (text-val in)]
      (is (= ex ou)))))

(deftest text-val-test-no
  (testing "section element with no text value"
    (let [in [[:title "title"] [:some-tag "tag"] [:toxt "text"]]
          ex nil
          ou (text-val in)]
      (is (= ex ou)))))

(deftest empty-sec?-test-yes
  (testing "a section with no child and blank text is empty"
    (let [in [ [:title "empty"] [:text "\n\n"] [:tag "no section"] ]
          ex true
          ou (empty-sec? in)]
      (is (= ex ou)))))

(deftest empty-sec?-test-no
  (testing "a section with non blanl text is not empty"
    (let [in [ [:title "empty"] [:text "text"] [:tag "no section"] ]
          ex false
          ou (empty-sec? in)]
      (is (= ex ou)))))

(deftest empty-sec?-test-no
  (testing "a section with subsection is not empty"
    (let [in [[:title "empty"] [:tag "no section"] 
              [:subs1 [:sub1 [:text "a"] [:title "title"]]]]
          ex false
          ou (empty-sec? in)]
      (is (= ex ou)))))


;filter function tests.
(def article-test 
  [:article
  [:abstract "Rich Hickey,the creator of the Clojure programming language.\n"]
  [:sections
     [:section
         [:title " Presentations "]
         [:text "some presentations"]]
     [:section [:title " References "]]
     [:section [:title "empty text no section"] [:text "\n\n"]]
     [:section [:title "section, empty text"] 
      [:subs1 [:sub1 [:title "subsection"] [:text "not blank"]]]]]])

(def article-test-del-empty-ex
  [:article
  [:abstract "Rich Hickey,the creator of the Clojure programming language.\n"]
  [:sections
     [:section
         [:title " Presentations "]
         [:text "some presentations"]]
     [:section [:title "section, empty text"] 
      [:sections [:section [:title "subsection"] [:text "not blank"]]]]]])

(def article-test-del-named-ex
  [:article
  [:abstract "Rich Hickey,the creator of the Clojure programming language.\n"]
  [:sections
     [:section [:title " Presentations "] [:text "some presentations"]]
     [:section [:title "section, empty text"]]]])

(deftest del-empty-test
  (testing "empty section must be deleted"
    (let [in article-test
          ex article-test-del-empty-ex
          ou (del-empty article-test)]
      (is (= ex ou)))))


(def del-sec-with-title-article-simple-in
  [:sections [:section [:title "BAD"] [:text "OFFENSIVE"]]])

(def del-sec-with-title-article-simple-ex
  [:sections])

(deftest del-sec-with-title-1
  (testing "tree containing only a section with a title that must be removed must not contain section"
    (let [in del-sec-with-title-article-simple-in
          ex del-sec-with-title-article-simple-ex
          ou (del-sec-with-title in #{"BAD"})]
      (is (= ex ou)))))

