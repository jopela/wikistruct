(ns wikison.filters-test
  (:require [clojure.test :refer :all]
            [wikison.filters :refer :all]
            [clojure.zip :as z]
            [clojure.string :as string]))

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

(def del-sec-with-title-2-in
  [:article [:abstract "abstract"]
   [:sections 
    [:section [:title "Литература"] [:text "text"]]]])

(def del-sec-with-title-2-ex
  [:article [:abstract "abstract"]])

(deftest del-sec-with-title-2
  (testing "When a russian article contains a bad section, it should be 
           removed"
    (let [in del-sec-with-title-2-in
          ex del-sec-with-title-2-ex
          ou (del-sec-with-title #{"Литература"} in)]
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

(deftest remove-bracket-1
  (testing "text containing bracket that contains 'Citation needed' or 
           number should be removed."
    (let [in "Sandra is 27 years old [Citation needed]. Wait it's cited here [1]"
          ex "Sandra is 27 years old . Wait it's cited here "
          ou (remove-brackets in)]
      (is (= ex ou)))))

; test cases for  removal of pronounciation content.
(def remove-pronounciation-text-in-1
  "boston (pronounced listeni/ˈbɒstən/) is the capital and largest city[9] of the state of massachusetts (officially the commonwealth of massachusetts), in the united states.")

(def remove-pronounciation-text-ex-1
  "boston is the capital and largest city[9] of the state of massachusetts, in the united states.")

(def remove-pronounciation-text-in-2
  "Canada /ˈkænədə/ is a country in North America consisting of 10 provinces and 3 territories.")

(def remove-pronounciation-text-ex-2
  "Canada is a country in North America consisting of 10 provinces and 3 territories.")
  
(def remove-pronounciation-text-in-3
  "France (UK /ˈfrɑːns/; US Listeni/ˈfræns/; French: [fʁɑ̃s] ( listen)), officially the French Republic (French: République française [ʁepyblik fʁɑ̃sɛz]), is a sovereign country in Western Europe that includes overseas regions and territories.")

(def remove-pronounciation-text-ex-3
  "France, officially the French Republic, is a sovereign country in Western Europe that includes overseas regions and territories.")

(def remove-pronounciation-text-in-4
  "Russia /ˈrʌʃə/ or /ˈrʊʃə/ (Russian: Россия, tr. Rossiya, IPA: [rɐˈsʲijə] ( listen)), also officially known as the Russian Federation[7] (Russian: Российская Федерация, tr.  Rossiyskaya Federatsiya, IPA: [rɐˈsʲijskəjə fʲɪdʲɪˈrat͡sɨjə] ( listen)), is a country situated in northern Eurasia.") 
(def remove-pronounciation-text-ex-4
  "Russia, also officially known as the Russian Federation[7], is a country situated in northern Eurasia.")

(def remove-pronounciation-text-in-5
  "Alberta /ælˈbɜrtə/ is a province of Canada.")

(def remove-pronounciation-text-ex-5
  "Alberta is a province of Canada.")

(def remove-pronounciation-text-in-6
  "Moscow (/ˈmɒskaʊ/ or /ˈmɒskoʊ/; Russian: Москва, tr. Moskva, IPA: [mɐˈskva] ( )) is the capital city and the most populous federal subject of Russia.")

(def remove-pronounciation-text-ex-6
  "Moscow is the capital city and the most populous federal subject of Russia.")

(deftest remove-pronounciation-test-1
  (testing "should remove all content in parens from text."
    (let [in remove-pronounciation-text-in-1
          ex remove-pronounciation-text-ex-1
          ou (remove-pronounciation-text in)]
      (is (= ex ou)))))

(deftest remove-pronounciation-test-2
  (testing "should remove all content in parens from text."
    (let [in remove-pronounciation-text-in-2
          ex remove-pronounciation-text-ex-2
          ou (remove-pronounciation-text in)]
      (is (= ex ou)))))

(deftest remove-pronounciation-test-3
  (testing "should remove all content in parens from text."
    (let [in remove-pronounciation-text-in-3
          ex remove-pronounciation-text-ex-3
          ou (remove-pronounciation-text in)]
      (is (= ex ou)))))

(deftest remove-pronounciation-test-4
  (testing "should remove all content in parens from text."
    (let [in remove-pronounciation-text-in-4
          ex remove-pronounciation-text-ex-4
          ou (remove-pronounciation-text in)]
      (is (= ex ou)))))

(deftest remove-pronounciation-test-5
  (testing "should remove all content in parens from text."
    (let [in remove-pronounciation-text-in-5
          ex remove-pronounciation-text-ex-5
          ou (remove-pronounciation-text in)]
      (is (= ex ou)))))

(deftest remove-pronounciation-test-6
  (testing "should remove all content in parens from text."
    (let [in remove-pronounciation-text-in-6
          ex remove-pronounciation-text-ex-6
          ou (remove-pronounciation-text in)]
      (is (= ex ou)))))

(def del-voir-in-1
  [:article
   [:abstract "Ca parle du Parc Jarry. Voir autre chose.\nIci au parc piscine"]
   [:sections
    [:section
     [:title "sec"]
     [:text "super text"]]]])

(def del-voir-ex-1
  [:article
     [:abstract "Ici au parc piscine"]
     [:sections
      [:section
       [:title "sec"]
       [:text "super text"]]]])

(deftest del-voir-test
  (testing "first sentences of abstract containing voir should be removed"
    (is (= del-voir-ex-1 (del-voir del-voir-in-1))))) 

; ~~~~~~~~~~~~ del-about test ~~~~~~~~~~~~~~~~~~~~~~~~~~
(def del-about-in-1
  [:article
   [:abstract "This article is about the church in Barcelona. For other uses, See.\n\nSanta Maria del Maria is ..."]
   [:sections
    [:section
     [:title "sec"]
     [:text "text"]]]])

(def del-about-ex-1
  [:article
   [:abstract "Santa Maria del Maria is ..."]
   [:sections
    [:section
     [:title "sec"]
     [:text "text"]]]])

(deftest del-about-text-1
  (testing "Content like 'This article is about' should be removed from article abstract"
    (is (= del-about-ex-1 (del-about del-about-in-1)))))

; ~~~~~~~~~~~~ del-pronounciation test ~~~~~~~~~~~~~~~~~~
(def del-pronounciation-in-1 
  [:article
   [:abstract remove-pronounciation-text-in-4]
   [:sections
    [:section 
     [:title "section title"] 
     [:text "some text"]]]])

(def del-pronounciation-ex-1 
  [:article
   [:abstract remove-pronounciation-text-ex-4]
   [:sections
    [:section 
     [:title "section title"] 
     [:text "some text"]]]])

(deftest del-pronounciation-1
  (testing "IPA char and related content should be removed from the first 
           sentence of the abstract text."
    (let [in del-pronounciation-in-1
          ex del-pronounciation-ex-1
          ou (del-pronounciation in)]
      (is (= ex ou)))))


(def remove-portail-in-1 "Ceci est l'article de Toronto. Portail de toronto. Bien.")
(def remove-portail-ex-1 "Ceci est l'article de Toronto. Bien.")

(deftest remove-portail-1
  (testing "Sentence containing the word Portail should be removed."
    (let [in remove-portail-in-1
          ex remove-portail-ex-1
          ou (remove-portail in)]
      (is (= ex ou)))))

(def remove-portail-in-2 "Ceci est l'article de Toronto. Portail de toronto\n")
(def remove-portail-ex-2 "Ceci est l'article de Toronto.")

(deftest remove-portail-2
  (testing "Sentence containing the word Portail  and ending with a newline 
           should be removed."
    (let [in remove-portail-in-2
          ex remove-portail-ex-2
          ou (remove-portail in)]
      (is (= ex ou)))))

(def remove-portail-in-3 "Ceci est l'article de Toronto. Portail de toronto")
(def remove-portail-ex-3 "Ceci est l'article de Toronto.")

(deftest remove-portail-3
  (testing "Sentence containing the word Portail at the end of a string 
           should be removed."
    (let [in remove-portail-in-3
          ex remove-portail-ex-3
          ou (remove-portail in)]
      (is (= ex ou)))))

(deftest remove-coordinates-text-1
  (testing "coordinates pair at the start of an article text shall be removed."
    (are [ex in] (= ex (remove-coordinates-text in))
         "Замок Святого Георгия — крепость в." "38.713889, -9.133611 Замок Святого Георгия — крепость в."
         "Then there is sentence." "-1.011111, 245.124323 Then there is sentence."
         "German coordinates" "45.504055-73.543911 German coordinates" )))

(deftest remove-plan-text-1
  (testing "should remove text containing Plan at the start of a line."
    (are [ex in] (= ex (remove-plan-text in))
         "" "Plan officiel de la ville de Berlin. La ville de."
         "" "Plan officiel de POI")))

(deftest remove-about-text-test-1
  (testing "sentences like 'This article is about' should be removed from text"
    (are [ex in] (= ex (remove-about-text in))
         "" "This article is about something. For other uses, see else.\n\n"
         "Santa Maria del Mar" "This article is about something else. For other uses, see (disam).\n\nSanta Maria del Mar")))

(deftest remove-brackets-all-test-1
  (testing "all closed bracket should be removed from the text."
    (are [ex in] (= ex (remove-brackets-all in))
         "Der parc im Stad" "Der parc [park del lol] im Stad"
         "Le parc cossin " "Le parc cossin [que lon doit aussi prononcer\npar cossin]")))

(deftest remove-short-lines-test
  (testing "all short lines should be removed"
    (are [ex in] (= ex (remove-short-lines in))
         "this is a long line" "f1\nthis is a long line"
         "" "f1")))
(deftest remove-single-word-line-test
  (testing "lines containing only a single word should be removed"
    (are [ex in] (= ex (remove-single-word-line in))
         "Der Gare Windsor" "i7i12i13i15i16i16i18i20\nDer Gare Windsor"
         "ok" "[word?]\nok" )))

(deftest remove-contains-word-test
  (testing "sentences containing word should be replaced by the empty string"
    (are [ex word text] (= ex (remove-contains-word word text))
         "" "voir" "Jarry c'est un beau parc! Voir aussi le parc x"
         "" "gustavo" "Walter White will be removed by gustavo?"
         "this is awesome" "good" "this is awesome")))

