(ns wikison.parse-test
  (:require [clojure.test :refer :all]
            [wikison.parse :refer :all]
            [instaparse.core :as insta]))

; test-extract tests.
(deftest parser-simple-1
  (testing "extract containing only an abstract"
    (let [in (slurp "./test/wikison/extracts/simple-test-1.txt")
          ex [:article [:abstract "Abstract only article.\n"]]
          ou (creole-parse in)]
      (is (= ex ou)))))

(deftest parser-simple-2
  (testing "abstract + some sections"
    (let [in (slurp "./test/wikison/extracts/simple-test-2.txt")
          ex [:article 
              [:abstract "Article with a single section.\n"]
              [:sections 
               [:section [:title " Single Section "] [:text "text.\n"]]]]
          ou (creole-parse in)]
      (is (= ex ou)))))

(deftest parser-simple-3
  (testing "several sections"
    (let [in (slurp "./test/wikison/extracts/simple-test-3.txt")
          ex [:article 
              [:abstract "Article with multiple sections.\n"]
              [:sections 
               [:section [:title " Section 1 "] [:text "text 1.\n"]]
               [:section [:title " Section 2 "] [:text "text 2.\n"]]
               [:section [:title " Section 3 "] [:text "text 3.\n"]]]]
          ou (creole-parse in)]
      (is (= ex ou)))))


(deftest parser-simple-4
  (testing "Section with subsections"
    (let [in (slurp "./test/wikison/extracts/simple-test-4.txt")
          ex [:article
              [:abstract "Article with subsection.\n\n"]
              [:sections 
               [:section 
                [:title " Section 1 "]
                [:text "will contain a sub.\n\n"]
                [:subs1 
                 [:sub1
                  [:title " Subsection 1 "][:text "Text.\n"]]]]]]
          ou (creole-parse in)]
      (is (= ex ou)))))

(deftest parser-simple-5
  (testing "All levels of indentation"
    (let [in (slurp "./test/wikison/extracts/simple-test-5.txt")
          ex [:article 
              [:abstract "All levels.\n"]
              [:sections
               [:section
                [:title " S1 "]
                [:subs1
                 [:sub1
                  [:title " SS1 "]
                  [:subs2
                   [:sub2 
                    [:title " SSS1 "]
                    [:subs3
                     [:sub3 
                      [:title " SSSS1 "]
                      [:subs4
                       [:sub4
                        [:title " SSSSS1 "]
                        [:subs5
                         [:sub5
                          [:title " SSSSSS1 "]]]]]]]]]]]]]]
          ou (creole-parse in)]
      (is (= ex ou)))))

;; grammar must be non-ambiguous for all test cases. THIS IS IMPORTANT for
;; deterministic results.
;(deftest non-ambiguous
;  (testing "non-ambiguity of grammer"
;    (let [ tests (vals test-cases)]
;      (doseq [t tests]
;        (let [results (insta/parses wiki-parser t)]
;          (is (= 1 (count results)))))))) 
;
;; edge case.
;(deftest edge-test-1
;  (testing "Abstract that contain code that is section-like (e.g: 1 == 1)"
;    (let [in (wiki-parser (slurp "./test/wikison/extracts/edge-test-1.txt"))
;          ex {:abstract "Complex\n" 
;              :sections [{:title " S1 " :text "in C : 1 == 1, in clojure (= 1 1).\n"}]}
;          ou (weval/tree-eval-clj in)]
;    (is (= ex ou)))))

