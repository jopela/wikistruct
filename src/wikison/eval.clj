(ns wikison.eval
  (:require [clojure.string :as string]
            [clojure.data.json :as json]
            [instaparse.core :as insta]))

(defn tree-eval-clj
  "evaluates the syntax tree into a clojure map-based data structure."
  [syntax-tree]
  (letfn [ (title [s] {:title s})
           (text  [& s] {:text  (apply str s)})
           (abstract [& s] {:abstract (apply str s)})
           (sections [& args] {:sections (vec args)}) ]
    (insta/transform {:title title
                      :sub1  merge
                      :sub2  merge
                      :sub3  merge
                      :sub4  merge
                      :sub5  merge
                      :section merge
                      :sections  sections
                      :subs1 sections
                      :subs2 sections
                      :subs3 sections
                      :subs4 sections
                      :subs5 sections
                      :abstract abstract
                      :sentence str
                      :text text
                      :article merge}
                     syntax-tree)))

      
