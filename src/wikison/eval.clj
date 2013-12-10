(ns wikison.eval
  "All the functions that can be used to turn a filtered abstract syntax
  tree into a concrete representation, such as:json, html, txt, 
  nothing (if you like abstract syntax tree like they are) clj data
  structure, anything !"
  (:require [clojure.string :as string]
            [clojure.data.json :as json]
            [instaparse.core :as insta]
            [clojure.zip :as z]
            [hiccup.core :as hiccup]))

(defn tree-eval-clj
  "evaluates the syntax tree into a clojure map-based data structure."
  [syntax-tree]
  (letfn [ (title [s] {:title s})
           (text  [& s] {:text  (apply str s)})
           (abstract [& s] {:abstract (apply str s)})
           (sections [& args] {:sections (vec args)})
           (article  [& args] {:article (apply merge args)})]

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
                      :article article}
                     syntax-tree)))

; helper function.
(defn translate-keyword
  "returns the right hiccup html keyword for the given argument."
  [kw]
  (condp = kw
    :article  :body
    :abstract :p
    :sections :div
    :section  :div
    :sub1     :div
    :sub2     :div
    :sub3     :div
    :sub4     :div
    :sub5     :div
    :subs1    :div
    :subs2    :div 
    :subs3    :div 
    :subs4    :div 
    :subs5    :div 
    :text     :p
    kw))

(defn heading
  "takes the location of a title in a syntax-tree and returns the appropriate
  html hx element associated with it."
  [title-loc]
  (let [sec-keyword (-> title-loc z/up z/up z/down z/node)]
    (condp = sec-keyword
      :section :h1
      :sub1    :h2
      :sub2    :h3
      :sub3    :h4
      :sub4    :h5
      :sub5    :h6)))

(defn translate-keyword-partial
  "returns the right html keyword for the given argument in the case of a 
  partial evaluation."
  [kw]
  (condp = kw
    :sub1     :div
    :sub2     :div
    :sub3     :div
    :sub4     :div
    :sub5     :div
    :subs1    :div
    :subs2    :div 
    :subs3    :div 
    :subs4    :div 
    :subs5    :div 
    :text     :p
    kw))

;~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
(defn rename-titles
  "translate the titles in the syntax tree according to the type of section
  they belong in."
  [syntax-tree]
  (loop [cur (z/vector-zip syntax-tree)]
    (let [node (z/node cur)]
      (cond
        (z/end? cur) (z/root cur)
        (= :title node) (recur (z/replace cur (heading cur)))
        :else (recur (z/next cur))))))

(defn rename-sections
  "rename different sections of the syntax tree into something more html."
  [syntax-tree]
  (loop [cur (z/vector-zip syntax-tree)]
    (let [node (z/node cur)]
      (cond
        (z/end? cur) (z/root cur)
        (keyword? node) (recur 
                          (z/next (z/replace cur (translate-keyword node))))
        :else (recur (z/next cur))))))

(defn tree-eval-html
  "evaluates the syntax tree into an html string using hiccup. Since this is 
  html, suppress newlines char."
  [syntax-tree]
  {:article (-> syntax-tree 
                rename-titles 
                rename-sections 
                hiccup/html
                (string/replace #"\n" " "))}) 

(defn tree-eval-html-partial
  "partially evaluates the syntax tree into html text. Useful when further 
  processing is required on some part of the tree."
  [syntax-tree]
  (let [renamed-tree (-> syntax-tree
                         rename-sections)]
    syntax-tree))

(defn tree-eval-identity
  "returns an article that contains the syntax tree itself."
  [syntax-tree]
  {:article syntax-tree})

