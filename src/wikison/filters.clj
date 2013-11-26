(ns wikison.filters
  "Collection of filter functions (and associated helper functions) applied 
  before the evaluation of the article tree into a concrete representation 
  (e.g: html, json etc.). A filter function could be, for example, a function 
  to remove unwanted sections or to remove certain words from the text."
  (:require [clojure.core.match :as match]
            [clojure.string :as string]
            [instaparse.core :as insta]
            [clojure.zip :as zip]))

; title we commonly want to remove
(def removable
  #{" References "
    " See also "})

; the article generation process is described by the following diagram
; raw-article --> parsing --> pre-process --> filtering --> post-filtering 
; --> evaluation.

; parsing: parses the article into an abstract syntax tree.
; pre-processing: perform operation such as merging sentences into text in 
; order to prepare for the filtering stage.
; filtering: prune the syntax tree of it's unwanted nodes.
; post-filtering: operations to be performed before evaluation.
; turns the resulting syntax tree into a concrete representation (json, html,
; text etc.)

; pre-process transform
(defn merge-sentence
  "merge sentences into text nodes"
  [syntax-tree]
  (insta/transform {:sentence str
                    :text (fn [& args] [:text (apply str args)] )}
                   syntax-tree))

;helper functions
(defn has-node?
  "Returns true if one of the element of the parse tree contains a node denoted
  by one the the keyword given in the ks set"
  [ks nodes]
  (some #(contains? ks (first %)) nodes))

(def has-child?
  (partial has-node? #{:sections :subs1 :subs2 :subs3 :subs4 :subs5}))

(defn text-val
  "Return the text value of the node. The text value of a node with no text
  is nil."
  [nodes]
  (letfn [(text-pass-filter [elem] (= (first elem) :text))]
    (let [filtered (filter text-pass-filter nodes)]
      (-> filtered first second))))

(defn empty-sec?
  "Returns true if the section is an empty one. Section emptyness is defined
  as having no child (subsections) and blank text."
  [nodes]
  (let [children? (has-child? nodes)
        blank-text? (-> nodes text-val string/blank?)]
    (and (not children?) blank-text?)))

(defn nil-empty-sec
  "nil the section if it empty"
  [& nodes]
  (if (empty-sec? nodes)
    nil
    (vec (cons :section nodes))))

(defn del-nil-sec
  "remove the nil from sections"
  [& nodes]
  (let [non-nil-sec (filter #(not (nil? %)) nodes)]
    (vec (cons :sections non-nil-sec))))

;remove empty section and empty sections
(defn del-empty
  "return a copy of the parse tree without the empty sections"
  [article]
  (insta/transform {:section nil-empty-sec 
                    :sub1    nil-empty-sec 
                    :sub2    nil-empty-sec 
                    :sub3    nil-empty-sec 
                    :sub4    nil-empty-sec 
                    :sub5    nil-empty-sec
                    :sections del-nil-sec
                    :subs1    del-nil-sec
                    :subs2    del-nil-sec
                    :subs3    del-nil-sec
                    :subs4    del-nil-sec
                    :subs5    del-nil-sec
                    } article))

; TODO: refactor these 2 functions.
(defn del-sec-with-title
  "entirely delete the section node that have a title that belong to one of the
  title set."
  [article ts]
  (loop [current (zip/vector-zip article)]
    (if (zip/end? current)
      (zip/root current)
      (let [current-node (zip/node current)]
        (if (and (= current-node :title) (-> current zip/right zip/node ts))
          (recur (-> current zip/up zip/up zip/remove))
          (recur (zip/next current)))))))

