(ns wikison.filters-test
  (:require [clojure.test :refer :all]
            [wikison.filters :refer :all]
            [clojure.zip :as zip]))

(def del-sec-with-title-1-in
  [:article [:abstract "abstract"]
   [:sections 
    [:section [:title "BAD"] [:text "text"]]]])

(def del-sec-with-title-1-ex
  [:article [:abstract "abstract"]])

(deftest del-sec-with-title-1
  (testing "tree containing only a section with a title that must be removed must not contain section"
    (let [in del-sec-with-title-1-in
          ex del-sec-with-title-1-ex
          ou (del-sec-with-title #{"BAD"} in)]
      (is (= ex ou)))))

; delete empty section test.
(def del-empty-sec-1-in
  [:article
   [:sections
    [:section [:title "title"] [:text "test"]
     [:sections
      [:section [:title "title1"] [:text "\n\n"]]]]]])
  
(def del-empty-sec-1-ex
  [:article
   [:sections
    [:section [:title "title"] [:text "test"]]]])

(deftest del-empty-sec-1
  (testing "section with blank text and no children should be removed."
    (let [in del-empty-sec-1-in
          ex del-empty-sec-1-ex
          ou (del-empty-sections in)]
      (is (= ex ou)))))

(def del-empty-sec-2-in
  [:article
   [:abstrat "A\n"]
   [:sections
    [:section [:title "S1"] [:text "\n\n"]
     [:subs1 
      [:sub1 [:title "DO"] [:text "text"]]]]]])

(def del-empty-sec-2-ex del-empty-sec-2-in)

(deftest del-empty-sec-2
  (testing "should not delete empty section that have children."
    (let [in del-empty-sec-2-in
          ex del-empty-sec-2-ex
          ou (del-empty-sections in)]
      (is (= ex ou)))))

(def del-empty-sec-3-in
  [:article
   [:abstract "A\n"]
   [:sections
    [:section
     [:title "T"]
     [:text "\n\n"]
     [:subs1
      [:sub1
       [:title "title"]
       [:text "\n"]]]]]])

(def del-empty-sec-3-ex
  [:article
   [:abstract "A\n"]])

(deftest del-empty-sec-3
  (testing "should remove nested empty subsection"
    (let [in del-empty-sec-3-in
          ex del-empty-sec-3-ex
          ou (del-empty-sections in)]
      (is (= ex ou)))))

(def del-empty-sec-4-in
  [:article 
   [:abstract "A\n"]
   [:sections
    [:section
     [:title "blank"]
     [:subs1
      [:sub1 
       [:title "blank"]
       [:subs2
        [:title "blank"]]]]]]])

(def del-empty-sec-4-ex
  [:article
   [:abstract "A\n"]])

(deftest del-empty-sec-4
  (testing "should remove nested empty section with no text node"
    (let [in del-empty-sec-4-in
          ex del-empty-sec-4-ex
          ou (del-empty-sections in)]
      (is (= ex ou)))))

(def del-empty-sec-5-in
  [:article
   [:abstract "A\n"]
   [:sections
    [:section
     [:title "title"]
     [:subs2
      [:sub2
       [:title "title"]
       [:subs3
        [:sub3
         [:title "title"]]]]]
     [:subs1
      [:sub1
       [:title "title"]
       [:text "text"]]]]]])

(def del-empty-sec-5-ex
  [:article
   [:abstract "A\n"]
   [:sections
    [:section
     [:title "title"]
     [:subs1
      [:sub1
       [:title "title"]
       [:text "text"]]]]]])

(deftest del-empty-sec-5
  (testing "empty sections container should be removed from syntax-tree"
    (let [in del-empty-sec-5-in
          ex del-empty-sec-5-ex
          ou (del-empty-sections in)]
      (is (= ex ou)))))

(deftest match-removable-1
  (testing "text that is similar to something in the removable set should 
           return logical true"
    (let [in " Articles connexes "
          ex "articles connexes"
          ou (match-removable? in)]
      (is (= ex ou)))))

