(ns wikison.core-test
  (:require [clojure.test :refer :all]
            [wikison.core :refer :all]))

(def whistlers-mother
  {:url         "http://en.wikipedia.org/wiki/Whistler's_Mother"
   :title       "Whistler's Mother"
   :pageid      54458
   :abstract    "Arrangement in Grey and Black ..."
   :depiction   "https://upload.wikimedia.org/wikipedia/commons/1/1b/Whistlers_Mother_high_res.jpg"
   :lang        "en"
   :other-langs ["da" "de" "es" "et" "fa" "fr" "he" "la" "nl" "pl"]
   :sections [{:name "History" :text "Anna McNeill Whistler ..." } 
              {:name "Appearances in American museums" :text "Whistler's Mother occasionally appears" }
              {:name "In popular culture" :text "The painting has been"}
              {:name "In music" :text "Whistler and particularly"}]
   })

(deftest article-test
  (testing "json document represents the article"
    (let [in  "http://en.wikipedia.org/wiki/Whistler's_Mother"
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

