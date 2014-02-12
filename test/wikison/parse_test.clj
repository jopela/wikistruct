(ns wikison.parse-test
  (:require [clojure.test :refer :all]
            [wikison.parse :refer :all]
            [instaparse.core :as insta]))

(def test-files-list
  ["./test/wikison/extracts/simple-test-1.txt"
   "./test/wikison/extracts/simple-test-2.txt"
   "./test/wikison/extracts/simple-test-3.txt"
   "./test/wikison/extracts/simple-test-4.txt"
   "./test/wikison/extracts/simple-test-5.txt"
   "./test/wikison/extracts/edge-test-1.txt"
   "./test/wikison/extracts/edge-test-2.txt"
   "./test/wikison/extracts/edge-test-3.txt"
   "./test/wikison/extracts/edge-test-4.txt"
   "./test/wikison/extracts/edge-test-5.txt"
   "./test/wikison/extracts/edge-test-6.txt"
   "./test/wikison/extracts/edge-test-7.txt"
   ])

; test-extract tests.
(deftest parser-simple-1
  (testing "extract containing only an abstract"
    (let [in (slurp "./test/wikison/extracts/simple-test-1.txt")
          ex [:article [:abstract "Abstract only article.\n\n"]]
          ou (creole-parse in)]
      (is (= ex ou)))))

(deftest parser-simple-2
  (testing "abstract + some sections"
    (let [in (slurp "./test/wikison/extracts/simple-test-2.txt")
          ex [:article 
              [:abstract "Article with a single section.\n"]
              [:sections 
               [:section [:title " Single Section "] [:text "text.\n\n"]]]]
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
               [:section [:title " Section 3 "] [:text "text 3.\n\n"]]]]
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
                  [:title " Subsection 1 "][:text "Text.\n\n"]]]]]]
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
                          [:title " SSSSSS1 "]
                          [:text "\n"]]]]]]]]]]]]]]
          ou (creole-parse in)]
      (is (= ex ou)))))


(deftest edge-test-1
  (testing "Abstract that contain code that is section-like (e.g: 1 == 1)"
    (let [in (slurp "./test/wikison/extracts/edge-test-1.txt")
          ex [:article
              [:abstract "Complex\n"] 
              [:sections
               [:section
                [:title " S1 "] 
                [:text "in C : 1 == 1, in clojure (= 1 1).\n\n"]]]]
          ou (creole-parse in)]
    (is (= ex ou)))))

(deftest edge-test-2
  (testing "Sections can contain any sublevel sections"
    (let [in (slurp "./test/wikison/extracts/edge-test-2.txt")
          ex [:article 
              [:abstract "Any kind of sub section.\n"]
              [:sections
               [:section
                [:title " Section 1 "] [:text "See sub-sub.\n"]
                [:subs2 
                 [:sub2
                  [:title " Sub-sub "] [:text "text\n\n"]]]]]]
          ou (creole-parse in)]
      (is (= ex ou)))))

(deftest edge-test-3
  (testing "section can contain different levels of lower subsections"
    (let [in (slurp "./test/wikison/extracts/edge-test-3.txt")
          ex [:article 
              [:abstract "Abstract\n"]
              [:sections
               [:section
                [:title " S1 "] [:text "text1\n"] 
                [:subs2 [:sub2 [:title " S2 "] [:text "text2\n"]]] 
                [:subs1 [:sub1 [:title " S3 "] [:text "text3\n\n\n"]]]]]]
          ou (creole-parse in)]
      (is (= ex ou)))))

(deftest edge-test-4
  (testing "Article content may be insde subs of any level after Abstract."
    (let [in (slurp "./test/wikison/extracts/edge-test-4.txt")
          ex [:article 
              [:abstract "Abstract\n"]
              [:subs1
               [:sub1
                [:title " S "] [:text "starting with sub1.\n\n"]]]]
          ou (creole-parse in)]
      (is (= ex ou)))))

(deftest edge-test-5
  (testing "Article having sections after subs1 should be persed correctly ."
    (let [in (slurp "./test/wikison/extracts/edge-test-5.txt")
          ex [:article 
              [:abstract "Abstract\n"]
              [:subs1
               [:sub1
                [:title " SU1 "] [:text "sub1.\n"]]
               [:sub1
                [:title " SU2 "] [:text "sub1.\n"]]]
              [:sections
               [:section
                [:title " S "] [:text "sec.\n\n"]]]]
          ou (creole-parse in)]
      (is (= ex ou)))))

(deftest edge-test-6
  (testing "Articles having an equal sign begin a section should be 
           parsed correctly."
    (let [in (slurp "./test/wikison/extracts/edge-test-6.txt")
          ex [:article 
              [:abstract "Abstract\n"]
              [:sections
               [:section
                [:title " S "] [:text "S1.\n"]
               [:subs1
                [:sub1
                 [:title " SS "] [:text "dim\n=3\n\n"]]]]]]

          ou (creole-parse in)]
      (is (= ex ou)))))

(deftest edge-test-7
  (testing "Articles starting with subs2 should be parsed correctly."
    (let [in (slurp "./test/wikison/extracts/edge-test-7.txt")
          ex [:article 
              [:abstract "Article\n"]
              [:subs2
               [:sub2
                [:title " SS1 "] [:text "s1\n"]]
               [:sub2
                [:title " SS2 "] [:text "s2\n"]]]
               [:sections
                [:section
                 [:title " S1 "] [:text "s3\n\n"]]]]

          ou (creole-parse in)]
      (is (= ex ou)))))

(deftest non-ambiguous
 (testing "grammar must be non-ambiguous for all test cases."
  (let [ins (map slurp test-files-list)]
    (doseq [in ins]
      (let [result (insta/parses wiki-parser in)]
        (is (= 1 (count result))))))))

