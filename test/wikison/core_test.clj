(ns wikison.core-test
  (:require [clojure.test :refer :all]
            [wikison.core :refer :all]
            [instaparse.core :as insta]))

(def user-agent-test "wikison 0.1.1 (jonathan.pelletier1@gmail.com)")

(def article-test-1-in 
  "http://en.wikipedia.org/wiki/iwillneverexist")

(def article-test-1-ex
  {:error 
   "resource  http://en.wikipedia.org/wiki/iwillneverexist  is missing"})

