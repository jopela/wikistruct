(ns wikison.core-test
  (:require [clojure.test :refer :all]
            [wikison.core :refer :all]))

; an example api call result
(def api-result {:langlinks [
{:lang "da", :* "Arrangement in Gray and Black: The Artist's Mother"}
{:lang "de", :* "Arrangement in Grau und Schwarz: Porträt der Mutter des Künstlers"}
{:lang "es", :* "Retrato de la madre del artista"}
{:lang "et", :* "Whistleri ema"}
{:lang "fa", :* "مادر ویسلر"}
{:lang "fr", :* "Arrangement en gris et noir n°1"}
{:lang "he", :* "סידור באפור ושחור: אמו של האמן"}
{:lang "la", :* "Mater pictoris (Whistler)"}
{:lang "nl", :* "Arrangement in Gray and Black, No. 1"}
{:lang "pl", :* "Matka Whistlera"}],
                :pagelanguage "en",
                :title "Whistler's Mother",
                :thumbnail {:source "https://upload.wikimedia.org/wikipedia/commons/1/1b/Whistlers_Mother_high_res.jpg", :width 9999, :height 8897}, 
                :extract "Arrangement in Grey and Black No.1,is a painting . 
                 It is now one of the most famous works by an American artist 
                 outside the United States. \n\n\n== History ==\n\nAnna McNeill
                         Whistler posed for the painting while living in London
                         with her son. \n\n\n== Appearances in American museums
                         ==\nWhistler's Mother occasionally appears in the 
                         United States.\n\n\n== In popular culture ==\nThe 
                         painting has been featured or mentioned in numerous
                         works of fiction and within pop culture.\n\n\n== In music ==\n
                         Whistler and particularly this painting had a profound
                         effect on Claude Debussy, a contemporary French 
                         composer.",
                 :fullurl "https://en.wikipedia.org/wiki/Whistler%27s_Mother",
                 :pageid 225516})

(def whistlers-mother
  {:url         "https://en.wikipedia.org/wiki/Whistler%27s_Mother"
   :title       "Whistler's Mother"
   :pageid      225516
   :abstract    "Arrangement in Grey and Black ..."
   :depiction   "https://upload.wikimedia.org/wikipedia/commons/1/1b/Whistlers_Mother_high_res.jpg"
   :lang        "en"
   :other-langs ["da" "de" "es" "et" "fa" "fr" "he" "la" "nl" "pl"]
   :sections [{:name "History" :text "Anna McNeill Whistler ..." } 
              {:name "Appearances in American museums" :text "Whistler's Mother occasionally appears" }
              {:name "In popular culture" :text "The painting has been"}
              {:name "In music" :text "Whistler and particularly"}]
   })

(deftest wiki-parse-simple-4
  (testing "parsing of an article that only contains an abstract"
    (let [in (slurp "/root/dev/wikison/test/wikison/extracts/simple-test-4.txt")
          ex [:article 
              [:abstract 
               [:line "This article only contains an abstract.\n"]]]
          ou (wiki-parser in)] 
      (is (= ex ou)))))

(deftest wiki-parse-ru-simple-1
  (testing "parsing of an article that only contains an abstract with ru char"
    (let [in (slurp "/root/dev/wikison/test/wikison/extracts/ru-simple-test-1.txt")
          ex [:article 
              [:abstract 
               [:line "привет мир от русских коды символов!\n"]]]
          ou (wiki-parser in)] 
      (is (= ex ou)))))

(deftest wiki-parse-simple-5
  (testing "parsing of an article that contain an abstract and a section"
    (let [in (slurp "/root/dev/wikison/test/wikison/extracts/simple-test-5.txt")
          ex [:article 
              [:abstract 
               [:line "This is the second simplest possible article. It will contain a section.\n"]]
              [:sep "\n\n"]
              [:section 
               [:heading "== " [:name "Section"] " ==" "\n"]
               [:line "Section text.\n"]]]
          ou (wiki-parser in)] 
      (is (= ex ou)))))

(deftest wiki-parse-simple-6
  (testing "parsing of an article that contain an abstract and a section
           title/heading having many words"
    (let [in (slurp "/root/dev/wikison/test/wikison/extracts/simple-test-6.txt")
          ex [:article 
              [:abstract 
               [:line "Test abstract.\n"]]
              [:sep "\n\n"]
              [:section 
               [:heading "== " [:name "Section"] " " [:name "title"] " ==" "\n"]
               [:line "Title with many words.\n"]]]
          ou (wiki-parser in)] 
      (is (= ex ou)))))

(deftest article-test
  (testing "json document represents the article"
    (let [in  "https://en.wikipedia.org/wiki/Whistler's_Mother"
          ex  whistlers-mother 
          ou  (article in) ]
      (is (= (ex :url) (ou :url)))
      (is (= (ex :title) (ou :title))))))

(deftest article-title-test
  (testing "title of a page from english wikipedia"
    (let [in "http://en.wikipedia.org/wiki/Our_Lady_of_the_Don"
          ex "Our_Lady_of_the_Don"
          ou (article-title in)]
      (is (= ex ou)))))

(deftest api-url-test
  (testing "api-url for wikipedia url"
    (let [in "http://en.wikipedia.org/wiki/Montreal"
          ex "http://en.wikipedia.org/w/api.php"
          ou (api-url in)]
      (is (= ex ou)))))

(deftest simple-prop-extract-test
  (testing "simple property extraction from request result on MediaWiki API"
    (let [ex (select-keys whistlers-mother [:url :title :pageid :lang]);{:url "https://en.wikipedia.org/wiki/Whistler%27s_Mother" :title "Whistler's Mother" :pageid 225516 :lang "en"}
          ou (simple-prop-extract api-result)]
      (is (= ex ou)))))

(deftest languages-extract-test
  (testing "language extraction from request result on MediaWiki API"
    (let [ex (select-keys whistlers-mother [:other-langs])
          ou (languages-extract api-result)]
      (is (= ex ou)))))

(deftest thumbnail-extract-test
  (testing "thumbnail extraction from request result on MediaWiki API"
    (let [ex (select-keys whistlers-mother [:depiction])
          ou (thumbnail-extract api-result)]
      (is (= ex ou)))))


