(ns wikison.core-test
  (:require [clojure.test :refer :all]
            [wikison.core :refer :all]
            [wikison.request :refer :all]
            [instaparse.core :as insta]))

(def user-agent-test "wikison 0.1.1 (jonathan.pelletier1@gmail.com)")

(def urltest1 "http://anonresolvablehostnameforyou.com")
(def unresolvable-err {:error (str "hostname for " urltest1 " could not be resolved")})
(deftest unresolvable-url
  (testing "an error keyword should be present in the result dictionary
           whenever the url hostname could not be resolved."
   (let  [in urltest1
          ex unresolvable-err
          ou (article user-agent-test in)]
      ; the error keyword must be present and the error strings must match.
      (is (and (ou :error) (= (ou :error) (ex :error)))))))

(def urltest2 "http://google.com/wiki/Montreal")
(def http-404-err {:error (str "query for " urltest2 " returned http error code 404")})
(deftest http-error-code
  (testing "error keyword text should indicate that there has been an error 
           when a server returns a 404 status code."
    (let [in urltest2
          ex http-404-err
          ou (article user-agent-test in)]
      (is (and (ou :error) (= (ou :error) (ex :error)))))))

(def urltest3 "http://en.wikipedia.org/wiki/Jonathan_Pelletier")
(def missing-page-err {:error (str "page for " urltest3 " does not exist on the queried wiki")})
(deftest missing-page
  (testing "error should be present when the wiki page is missing"
    (let [in urltest3
          ex missing-page-err
          ou (article user-agent-test in)]
      (is (and (ou :error) (= (ou :error) (ex :error)))))))

(def urltest4 "http://en.wikipedia.org/wiki/bad")
(def wiki-parsing-err {:error (str "wiki-creole parsing error for " urltest4)})
(deftest invalid-creole 
  (testing "proper error string should indicate that wiki creole parsing failed
           when it happens"
    (with-redefs [raw-article (constantly {:editurl "http://en.wikipedia.org/w/index.php?title=Rich_Hickey&action=edit", :lastrevid 586090578, :ns 0, :length 1460, :pagelanguage "en", :title "Rich Hickey", :thumbnail {:source "http://upload.wikimedia.org/wikipedia/commons/f/f4/Rich_Hickey.jpg", :width 9999, :height 7188}, :extract "Rich Hickey is the creator of the Clojure program==$34===23========ming language", :fullurl "http://en.wikipedia.org/wiki/Rich_Hickey", :pageid 20746480, :contentmodel "wikitext", :touched "2013-12-14T20:24:56Z", :counter ""})]
      (let [in urltest4 
            ex wiki-parsing-err
            ou (article user-agent-test in)]
      (is (and (ou :error) (= (ou :error) (ex :error))))))))

(def urltest5 "ajasjsjjajaj")
(def malformed-err {:error (str urltest5 " is a malformed url")})
(deftest malformed-url
  (testing "a malformed url should generate an error dictionary, not blow 
           up the system with an exception"
    (let [in urltest5
          ex malformed-err
          ou (article user-agent-test in)]
      (is (and (ou :error) (= (ou :error) (ex :error)))))))

        
