(ns wikison.filters-test
  (:require [clojure.test :refer :all]
            [wikison.filters :refer :all]
            [clojure.zip :as z]))

(deftest value-of-match-test-1
  (testing "when there is a match, should return the value"
    (let [in [:matchme "value"]
          ex "value"
          ou (value-of-match #{:matchme} in)]
      (is (= ex ou)))))

(deftest value-of-match-test-2
  (testing "when there is no match, should return the nil"
    (let [in [:matchme "value"]
          ex nil
          ou (value-of-match #{:matchyou} in)]
      (is (= ex ou)))))

(deftest value-of-match-test-3
  (testing "when given a non-corresponding pattern, should return nil"
    (let [in [:article [:abstract "something"]]
          ex nil
          ou (value-of-match #{:anything} in)]
      (is (= ex ou)))))

(deftest section-text?-1 
  (testing "section containing non-empty text should return logical true"
    (let [in (z/vector-zip [:section [:title "title"] [:text "text"]])
          ex true
          ou (section-text? in)]
      (is (and ex ou)))))

(deftest section-text?-2
  (testing "section containing empty text should return logical false"
    (let [in (z/vector-zip [:section [:title "title"] [:text "\n\n"]])
          ex nil
          ou (section-text? in)]
      (is (= ex ou)))))

(deftest section-text?-3
  (testing "section containing no text should return logical false"
    (let [in (z/vector-zip [:section [:title "title"] [:subs1
                                                       [:sub1 
                                                        [:title "t"]
                                                        [:text "text"]]]])
          ex nil
          ou (section-text? in)]
      (is (= ex ou)))))

(def container-node-locs-test-1-in
  (z/vector-zip [:section 
                    [:title "title"]
                    [:text "text"]
                    [:subs1
                     [:sub1
                      [:title "title"]
                      [:text "text"]]]]))

(def container-node-locs-test-1-ex
  [:subs1
   [:sub1
    [:title "title"]
    [:text "text"]]])

(deftest container-node-locs-test-1
  (testing "should return container subtrees when a section has containers"
    (let [in container-node-locs-test-1-in
          ex container-node-locs-test-1-ex
          ou (-> in container-node-locs first z/up z/node)]
      (is (= ex ou)))))

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


(def del-empty-sec-6-in
  [:article
   [:abstract "a"
    [:sections
     [:section 
      [:title "title"]
      [:text "\n\n"]]
     [:section
      [:title "title"]
      [:text "text"]]]]])

(def del-empty-sec-6-ex
  [:article 
   [:abstract "a"
    [:sections
     [:section
      [:title "title"]
      [:text "text"]]]]])

(deftest del-empty-sec-6
  (testing "empty individual section surrounded by non-empty section should
           be removed"
    (let [in del-empty-sec-6-in
          ex del-empty-sec-6-ex
          ou (del-empty-sections in)]
      (is (= ex ou)))))

(deftest match-removable-1
  (testing "text that is similar to something in the removable set should 
           return logical true"
    (let [in " Articles connexes "
          ex "articles connexes"
          ou (match-removable? in)]
      (is (= ex ou)))))

(deftest remove-bracket-1
  (testing "text containing bracket that contains 'Citation needed' or 
           number should be removed."
    (let [in "Sandra is 27 years old [Citation needed]. Wait it's cited here [1]"
          ex "Sandra is 27 years old . Wait it's cited here "
          ou (remove-brackets in)]
      (is (= ex ou)))))

