(ns wikison.filters
  "Collection of filter functions (and associated helper functions) applied 
  before the evaluation of the article tree into a concrete representation 
  (e.g: html, json etc.). A filter function could be, for example, a function 
  to remove unwanted sections or to remove certain words from the text."
  (:require [clojure.core.match :as match]
            [clojure.string :as string]
            [instaparse.core :as insta]
            [clojure.zip :as zip]))

; helper functions



;(defn subsections?
;  "Returns true if the loc points to a section that has subsection. nil if
;  not."
;  [loc]
;  (let [siblings (rights loc)]



; filter functions.
(defn del-sec-with-title
  "entirely delete the section node that have a title that belong to one of the
  title set."
  [article ts]
  (loop [current (zip/vector-zip article)]
    (if (zip/end? current)
      (zip/root current)
      (let [current-node (zip/node current)]
        (if [(and (= current-node :title) (-> current zip/right zip/node ts))]
          (recur (-> current zip/up zip/remove))
          (recur (zip/next current)))))))

(defn del-empty-sec
  "remove section that have blank text and no child"
  [article]
  article)


