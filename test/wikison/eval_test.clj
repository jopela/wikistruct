(ns wikison.eval-test
  (:require [clojure.test :refer :all]
            [wikison.eval :refer :all]
            [instaparse.core :as insta]
            [clojure.zip :as z]))

(def heading-test-1-in
  (-> [:section [:title "title"] [:text "text"]] 
      z/vector-zip 
      z/down 
      z/right 
      z/down))
  
(def heading-test-1-ex
  :h1)

(deftest heading-test
  (testing "syntax tree should be transformed into clj hash data structure"
    (let [in heading-test-1-in
          ex heading-test-1-ex
          ou (heading in)]
      (is (= ex ou)))))

(def rename-titles-1-in
  [:article
   [:abstract "abstract\n"]
   [:sections
    [:section
     [:title "title"] [:text "text"]
     [:subs1
      [:sub1
       [:title "sub1"] [:text "subtext"]]]]]])

(def rename-titles-1-ex
  [:article
   [:abstract "abstract\n"]
   [:sections
    [:section
     [:h1 "title"] [:text "text"]
     [:subs1
      [:sub1
       [:h2 "sub1"] [:text "subtext"]]]]]])

(deftest rename-titles-test-1
  (testing "title under section become h1 and title under sub1 become h2"
    (let [in rename-titles-1-in
          ex rename-titles-1-ex
          ou (rename-titles in)]
      (is (= ex ou)))))

(def rename-sections-1-in
  [:article 
   [:abstract "lol"]
   [:sections
    [:section [:title "title"] [:text "text"]]]])

(def rename-sections-1-ex
  [:body
   [:p "lol"]
    [:div
     [:div
      [:title "title"] [:p "text"]]]])

(def tree-eval-partial-test-1-in
  [:article 
   [:abstract "introduction"]
   [:sections
    [:section
     [:title "s1"]
     [:text "a"]
     [:subs1
      [:sub1 
       [:title "ss1"]
       [:text "b"]]
      [:sub1
       [:title "ss2"]
       [:text "c"]
       [:subs2
        [:sub2
         [:title "sss1"]
         [:text "d"]]]]]]]])
      
(def tree-eval-partial-test-1-ex
  {:article [:article
   [:abstract "introduction"]
   [:sections
    [:section
     [:title "s1"]
     [:text "<p>a</p><div><div><h2>ss1</h2><p>b</p></div><div><h2>ss2</h2><p>c</p><div><div><h3>sss1</h3><p>d</p></div></div></div></div>"]]]]})

(deftest tree-eval-partial-test-1
  (testing "should partially render syntax-tree up to the section level"
    (let [in tree-eval-partial-test-1-in
          ex tree-eval-partial-test-1-ex
          ou (tree-eval-html-partial in)]
      (is (= ex ou)))))

(def edit-subs-test-1-in tree-eval-partial-test-1-in)

(def edit-subs-test-1-ex
  [:article
   [:abstract "introduction"]
   [:sections
    [:section
     [:title "s1"]
     [:text "a"]
     [:markdown "<div><div><h2>ss1</h2><p>b</p></div><div><h2>ss2</h2><p>c</p><div><div><h3>sss1</h3><p>d</p></div></div></div></div>"]]]])

(deftest edit-subs-test-1
  (testing "should transform any subsection of a section into a text node."
    (let [in edit-subs-test-1-in
          ex edit-subs-test-1-ex
          ou (edit-subs in)]
      (is (= ex ou)))))

(def markdown-text-test-1-in edit-subs-test-1-ex)

(def markdown-text-test-1-ex 
  [:article
   [:abstract "introduction"]
   [:sections
    [:section
     [:title "s1"]
     [:markdown "<p>a</p>"]
     [:markdown "<div><div><h2>ss1</h2><p>b</p></div><div><h2>ss2</h2><p>c</p><div><div><h3>sss1</h3><p>d</p></div></div></div></div>"]]]])

(deftest mardown-text-test-1
  (testing "should wrap remaining text into a paragraph"
    (let [in markdown-text-test-1-in 
          ex markdown-text-test-1-ex
          ou (markdown-text in)]
      (is (= ex ou)))))

(def edit-subs-test-2-in 
  [:article 
   [:abstract "introduction"]
   [:sections
    [:section
     [:title "s1"]
     [:text "a"]
     [:subs2
      [:sub2 
       [:title "ss1"]
       [:text "b"]]
      [:sub2
       [:title "ss2"]
       [:text "c"]
       [:subs3
        [:sub3
         [:title "sss1"]
         [:text "d"]]]]]]]])

(def edit-subs-test-2-ex 
  [:article
   [:abstract "introduction"]
   [:sections
    [:section
     [:title "s1"]
     [:text "a"]
     [:markdown "<div><div><h3>ss1</h3><p>b</p></div><div><h3>ss2</h3><p>c</p><div><div><h4>sss1</h4><p>d</p></div></div></div></div>"]]]])

(deftest edit-subs-test-2
  (testing "Should transform any kind of subs of a section into a text node."
    (let [in edit-subs-test-2-in
          ex edit-subs-test-2-ex
          ou (edit-subs in)]
      (is (= ex ou)))))

(def sehp1-in
     [:subs1
      [:sub1 
       [:title "ss1"]
       [:text "b"]]
      [:sub1
       [:title "ss2"]
       [:text "c"]
       [:subs2
        [:sub2
         [:title "sss1"]
         [:text "d"]]]]])

(def sehp1-ex
  [:markdown "<div><div><h2>ss1</h2><p>b</p></div><div><h2>ss2</h2><p>c</p><div><div><h3>sss1</h3><p>d</p></div></div></div></div>"])

(deftest subtree-eval-html-partial-1
  (testing "should turn subtree into pure textnode"
    (let [in sehp1-in
          ex sehp1-ex
          ou (subtree-eval-html-partial in)]
      (is (= ex ou)))))

(deftest rename-sections-test-1
  (testing "things should be renamed accordingly"
    (let [in rename-sections-1-in
          ex rename-sections-1-ex
          ou (rename-sections in)]
      (is (= ex ou)))))

(def merge-section-test-1-in
  [[:title "title"] [:markdown "<p>a</p>"] [:markdown "<div>b</div>"]])

(def merge-section-test-1-ex
  [[:title "title"] [:text "<p>a</p><div>b</div>"]])

(deftest merge-section-test-1
  (testing "markdown section should be merged back into text"
    (let [in merge-section-test-1-in
          ex merge-section-test-1-ex
          ou (merge-section in)]
      (is (= ex ou)))))

