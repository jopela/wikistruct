(ns wikison.eval-test
  (:require [clojure.test :refer :all]
            [wikison.eval :refer :all]
            [instaparse.core :as insta]
            [clojure.zip :as z]))


(def heading-test-1-in
  (-> [:section [:title "title"] [:text "text"]] z/vector-zip z/down z/right z/down))
  
(def heading-test-1-ex
  :h1)

(deftest heading-test
  (testing "syntax tree should be transformed into clj hash data structure"
    (let [in heading-test-1-in
          ex heading-test-1-ex
          ou (heading in)]
      (is (= ex ou)))))

(def rename-titles-1-in
  [:article
   [:abstract "abstract\n"]
   [:sections
    [:section
     [:title "title"] [:text "text"]
     [:subs1
      [:sub1
       [:title "sub1"] [:text "subtext"]]]]]])

(def rename-titles-1-ex
  [:article
   [:abstract "abstract\n"]
   [:sections
    [:section
     [:h1 "title"] [:text "text"]
     [:subs1
      [:sub1
       [:h2 "sub1"] [:text "subtext"]]]]]])

(deftest rename-titles-test-1
  (testing "title under section become h1 and title under sub1 become h2"
    (let [in rename-titles-1-in
          ex rename-titles-1-ex
          ou (rename-titles in)]
      (is (= ex ou)))))

(def rename-sections-1-in
  [:article 
   [:abstract "lol"]
   [:sections
    [:section [:title "title"] [:text "text"]]]])

(def rename-sections-1-ex
  [:body
   [:p "lol"]
    [:div
     [:div
      [:title "title"] [:p "text"]]]])

(deftest rename-sections-test-1
  (testing "things should be renamed accordingly"
    (let [in rename-sections-1-in
          ex rename-sections-1-ex
          ou (rename-sections in)]
      (is (= ex ou)))))

