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
                 "bearbeiten" "annexe" "external links" "see also" })

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

(defn del-sec-with-title
  "delete the section node that have a title for which tf (title function)
  returns true"
  [tf syntax-tree]
  (loop [cur (z/vector-zip syntax-tree)]
    (let [node (z/node cur)]
      (cond 
        (z/end? cur) (z/root cur)
        (and (= node :title) (-> cur z/right z/node tf)) (recur (-> cur z/up z/up z/remove))
        (and (= node :sections) (-> cur z/right not)) (recur (-> cur z/up z/remove))
        :else (recur (z/next cur))))))

(def del-unwanted-sec (partial del-sec-with-title match-removable?)) 

(defn container?
  "returns logical true if the node is labelled as a container."
  [node]
  (let [containers #{:sections :subs1 :subs2 :subs3 :subs4 :subs5}]
    (containers node)))

(defn empty-container?
  "returns logical true if the container only contains empty section."
  [loc]
  false)

(defn del-empty-sections
  "remove empty sections container. The definition of emptyness is this 
  function precisely."
  [syntax-tree]
  (loop [cur (z/vector-zip syntax-tree)]
    (let [node (z/node cur)]
      (cond 
        (z/end? cur) (z/root cur)
        (and (container? node) (empty-container? cur)) (recur (z/remove cur))
        :else (recur (z/next cur))))))
