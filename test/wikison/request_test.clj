(ns wikison.request-test
  (:require [clojure.test :refer :all]
            [wikison.request :refer :all]))


(def test-user-agent "wikison v0.1.1 (jonathan.pelletier1@gmail.com)")

(deftest api-url-test
  (testing "api-url for wikipedia url"
    (let [in "http://en.wikipedia.org/wiki/Montreal"
          ex "http://en.wikipedia.org/w/api.php"
          ou (api-url in)]
      (is (= ex ou)))))

(deftest article-title-test
  (testing "title of a page from english wikipedia"
    (let [in "http://en.wikipedia.org/wiki/Our_Lady_of_the_Don"
          ex "Our_Lady_of_the_Don"
          ou (article-title in)]
      (is (= ex ou)))))

(deftest url-pageid-test-1
  (testing "when curid is present, it must be returned"
    (let [in "http://en.wikipedia.org/wiki/wiki/index.php?curid=10&sponges=1"
          ex "10"
          ou (url-pageid in)]
      (is (= ex ou)))))

(deftest url-pageid-test-2
  (testing "when curid is not present, it should return nil"
    (let [in "http://en.wikipedia.org/wiki/Montreal"
          ex nil
          ou (url-pageid in)]
      (is (= ex ou)))))

(deftest url-pageid-test-3
  (testing "when curid is not present, it should return nil. Even when other
           params are present"
    (let [in "http://en.wikipedia.org/wiki/index.php?curiosity=1000"
          ex nil
          ou (url-pageid in)]
      (is (= ex ou)))))

(deftest raw-article-test-1
  (testing "trying to fetch an article either by id or by title should get the
           exact same result"
    (let [in-1 "http://en.wikipedia.org/wiki/index.php?curid=2997438"
          in-2 "http://en.wikipedia.org/wiki/Club_Passim"
          ou-1 (raw-article test-user-agent in-1)
          ou-2 (raw-article test-user-agent in-2)]
      (is (= ou-1 ou-2)))))

