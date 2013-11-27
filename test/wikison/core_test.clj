(ns wikison.core-test
  (:require [clojure.test :refer :all]
            [wikison.core :refer :all]
            [instaparse.core :as insta]))

(def testfiles
  ["en-test-1.txt"
   "es-test-1.txt"
   "fr-test-1.txt"
   "it-test-1.txt"
   "ja-test-1.txt"
   "nl-test-1.txt"
   "pt-test-1.txt"
   "ru-test-1.txt"
   "edge-test-1.txt"
   "edge-test-2.txt"])

(defn load-tests
  [files]
  (let [path "./test/wikison/extracts/"]
    (letfn [(load-test [file] {(keyword file) (slurp (str path file))})]
      (apply merge (map load-test files)))))

(def test-cases (load-tests testfiles))

; test-extract tests.
(deftest parser-simple-1
  (testing "extract containing only an abstract"
    (let [in (wiki-parser (slurp "./test/wikison/extracts/simple-test-1.txt"))
          ex {:abstract "Abstract only article.\n"}
          ou (text-eval in)]
      (is (= ex ou)))))

(deftest parser-simple-2
  (testing "abstract + some sections"
    (let [in (wiki-parser (slurp "./test/wikison/extracts/simple-test-2.txt"))
          ex {:abstract "Article with a single section.\n"
              :sections [{:title " Single Section " :text "text.\n"}]}
          ou (text-eval in)]
      (is (= ex ou)))))

(deftest parser-simple-3
  (testing "several sections"
    (let [in (wiki-parser (slurp "./test/wikison/extracts/simple-test-3.txt"))
          ex {:abstract "Article with multiple sections.\n"
              :sections [{:title " Section 1 " :text "text 1.\n"}
                         {:title " Section 2 " :text "text 2.\n"}
                         {:title " Section 3 " :text "text 3.\n"}]}
          ou (text-eval in)]
      (is (= ex ou)))))

(deftest parser-simple-4
  (testing "Section with subsections"
    (let [in (wiki-parser (slurp "./test/wikison/extracts/simple-test-4.txt"))
          ex {:abstract "Article with subsection.\n\n"
              :sections [{:title " Section 1 " 
                          :text "will contain a sub.\n\n"
                          :sections [{:text "Text.\n"
                                      :title " Subsection 1 "}]}]}
          ou (text-eval in)]
      (is (= ex ou)))))

; thats unreadable.
(deftest parser-simple-5
  (testing "All levels of indentation"
    (let [in (wiki-parser (slurp "./test/wikison/extracts/simple-test-5.txt"))
          ex {:abstract "All levels.\n\n"
              :sections [{:title " S1 "
                          :sections 
                          [{:title " SS1 "
                                      :sections 
                            [{:title " SSS1 "
                                                  :sections 
                              [{:title " SSSS1 "
                                                              
                                :sections [{:title " SSSSS1 "
                                                                          
                                            :sections [{:title 
                                                      " SSSSSS1 "}]}]}]}]}]}]} 
          ou (text-eval in)]
      (is (= ex ou)))))

; russian text parsing test.
(def ru-exp-1 
  {:abstract "(лат. Tiberius Claudius Drusus),годы — Тибе́рий Кла́вдий Неро́н \n"
   :sections [
              {:title " Ученый " :text "В 14 году, когда скончался Август, Клавдий был избран главой посольства к \n"}
              {:title " Секретариат " :text "В первые годы своего правления Клавдий организовал императорский ."}]})

(deftest parse-ru-test-1
  (testing "russian article."
    (let [in (wiki-parser (slurp "./test/wikison/extracts/ru-test-1.txt"))
          ex ru-exp-1
          ou (text-eval in)]
    (is (= ex ou)))))

(def en-exp-1 
  {:abstract "Peercoin (code: PPC), also known as PPCoin and Peer-to-Peer Coin is the first \n"
   :sections [{:title " Transactions " :text "A peer-to-peer network handles Peercoin's transactions.\n\n" 
               :sections [{:title " Addresses " :text "digital signatures. \n"} 
                          {:title " Confirmations " :text "Transactions are recorded in the Peercoin blockchain.\n"}]}
              {:title " Distinguishing features " :text "\n\n" 
               :sections 
               [{:title " Proof-of-stake " :text "Peercoin's major distinguishing feature is that it uses\n\n\n"} 
                {:title " Proof-of-work " :text "SHA-256. For each 16x increase in the network, the PoW block reward is halved.\n"}]}
              ]})

(def es-exp-1
  {:abstract "Los escarabeoideos (Scarabaeoidea) son una superfamilia de coleópteros \n" 
   :sections [
               {:title " Características " :text "Las características distintivas de este vasto grupo son las antenas terminadas \n"}
               {:title " Historia natural " :text "(Rutelinae, Cetoniinae, Melolonthinae), etc. \n"}
               {:title " Referencias "}]})

(deftest parse-es-test-1
  (testing "spanish article"
    (let [in (wiki-parser (slurp "./test/wikison/extracts/es-test-1.txt"))
          ex es-exp-1
          ou (text-eval in)]
    (is (= ex ou)))))

; grammar must be non-ambiguous for all test cases. THIS IS IMPORTANT for
; deterministic results.
(deftest non-ambiguous
  (testing "non-ambiguity of grammer"
    (let [ tests (vals test-cases)]
      (doseq [t tests]
        (let [results (insta/parses wiki-parser t)]
          (is (= 1 (count results)))))))) 

; edge case.
(deftest edge-test-1
  (testing "Abstract that contain code that is section-like (e.g: 1 == 1)"
    (let [in (wiki-parser (slurp "./test/wikison/extracts/edge-test-1.txt"))
          ex {:abstract "Complex\n" 
              :sections [{:title " S1 " :text "in C : 1 == 1, in clojure (= 1 1).\n"}]}
          ou (text-eval in)]
    (is (= ex ou)))))

; other function.
;(deftest article-test
;  (testing "json document represents the article"
;    (let [in  "https://en.wikipedia.org/wiki/Whistler's_Mother"
;          ex  whistlers-mother 
;          ou  (article "wikison-test-suit (pelletier@mtrip.com)" in) ]
;      (is (= (ex :url) (ou :url)))
;      (is (= (ex :title) (ou :title))))))




