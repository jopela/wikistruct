(ns wikison.filters
  "Collection of filter functions (and associated helper functions) applied 
  before the evaluation of the article tree into a concrete representation 
  (e.g: html  json etc.). A filter function could befor example  a function 
  to remove unwanted sections or to remove certain words from the text."
  (:require [clojure.core.match :as match]
            [clojure.string :as string]
            [instaparse.core :as insta]
            [clojure.zip :as z]))

; helper functions
; title we commonly want to remove. To be used as tf passed to 
; del-sec-with-title.
(def removable #{
                 "pour approfondir" "note" "collega" "voci" "further reading"
                 "gallery" "weblinks" "einzelnachweise" "altri progetti" 
                 "referencias" "bibliography" "enlaces" "notas" "refer" 
                 "articles" "siehe" "véase" "galleria" "ver tam" 
                 "bakgrunnsstoff" "annexes" "images" "galerie" "anhang" 
                 "sources" "hauptartikel" "article détaillé"  "artículo" 
                 "literature" "literatur" "références" "externe" "fotogaleri" 
                 "anmerkungen" "sieheauch" "links" "source" 
                 "fuente" "zie ook" "quellen" 
                 "ligações externas" "referências" "bibliographie" 
                 "articles connexes" "liens externes" "editar"
                 "bearbeiten" "annexe" "external links" })

; function that can be used 
(defn match-any?
  "Returns logical true if (-> txt trim lower-case) belongs to rs"
  [rs txt]
  (let [cmp-txt (-> txt string/trim string/lower-case)]
    (rs cmp-txt)))

(def match-removable? (partial match-any? removable))

; the article generation process is described by the following diagram
; raw-article --> parsing --> pre-process --> filtering --> post-filtering 
; --> evaluation.

; parsing: parses the article into an abstract syntax tree.
; pre-processing: perform operation such as merging sentences into text in 
; order to prepare for the filtering stage.
; filtering: prune the syntax tree of it's unwanted nodes.
; post-filtering: operations to be performed before evaluation.
; turns the resulting syntax tree into a concrete representation (json  html 
; text etc.)

; pre-process transform
(defn merge-sentence
  "merge sentences into text nodes"
  [syntax-tree]
  (insta/transform {:sentence str
                    :text (fn [& args] [:text (apply str args)] )}
                   syntax-tree))

(defn del-sec-with-title
  "delete the section node that have a title for which tf (title function)
  returns true"
  [tf article]
  (loop [cur (z/vector-zip article)]
    (let [node (z/node cur)]
      (cond 
        (z/end? cur) (z/root cur)
        (and (= node :title) (-> cur z/right z/node tf)) (recur (-> cur z/up z/up z/remove))
        (and (= node :sections) (-> cur z/right not)) (recur (-> cur z/up z/remove))
        :else (recur (z/next cur))))))

(def del-unwanted-sec (partial del-sec-with-title match-removable?)) 

; helper for del-empty-sec.
(defn has-section?
  "returns logical true if the location of the given :section in the tree
  has subsections"
  [loc]
  (let [siblings (z/rights loc)]
    (letfn [(non-empty-sections? [s] (and (= (first s) :sections) (-> s second empty? not)))]
      (some non-empty-sections? siblings))))

(defn del-empty-sec
  "remove section that have blank text and no child"
  [article]
  (loop [cur (z/vector-zip article)]
    (let [node (z/node cur)]
      (cond 
        (z/end? cur) (z/root cur)
        (and (= node :text) (-> cur z/right z/node string/blank?) (-> cur z/up z/up z/down has-section? not)) (recur (-> cur z/up z/up z/remove))
        (and (= node :sections) (-> cur z/right not)) (recur (-> cur z/up z/remove))
        :else (recur (z/next cur))))))


