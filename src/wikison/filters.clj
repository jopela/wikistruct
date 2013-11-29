(ns wikison.filters
  "Collection of filter functions (and associated helper functions) applied 
  before the evaluation of the article tree into a concrete representation 
  (e.g: html  json etc.). A filter function could befor example  a function 
  to remove unwanted sections or to remove certain words from the text."
  (:require [clojure.core.match :as match]
            [clojure.string :as string]
            [instaparse.core :as insta]
            [clojure.zip :as z]))

; ~~~~~~~~~~~~~~~~~~~~~~~~~~ helper functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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

(defn value-of-match
  "takes the given node and tries to match it for a [k v] pattern. if it 
  matches and k is in the kws set, return the value of v. Else, return nil."
  [kws node]
  (match/match node
    [k v] (if (kws k) v nil)
    :else nil))

(defn match-any?
  "Returns logical true if (-> txt trim lower-case) belongs to rs"
  [rs txt]
  (let [cmp-txt (-> txt string/trim string/lower-case)]
    (rs cmp-txt)))

(def match-removable? (partial match-any? removable))

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
; ~~~~~~~~~~~~~~~~~ functions related to del-empty-sections ~~~~~~~~~~~~~~~~~~~
(def container? #{:sections :subs1 :subs2 :subs3 :subs4 :subs5})
(def section? #{:section :sub1 :sub2 :sub3 :sub4 :sub5})

(def not-blank? (complement string/blank?))

(defn section-text?
  "returns logical true if the given loc of the section contains a text node
  and that it's not blank. By loc of the section i mean a loc to something
  like [:section [:title 'title'] [:text 'text']]"
  [loc]
  (let [child-nodes (z/children loc)
        text? (comp not-blank? (partial value-of-match #{:text}))]
    (some text? child-nodes)))

(def no-section-text?
  (complement section-text?))

; input is loc of something like [:section [:title "title"] [:text "text"]]
; output should be the loc of the container tag 
(defn container-node-locs
  "returns a list of the container nodes location of the given section 
  location. Return value is a loc to the actual container tag (e.g: :sections)"
  [loc]
    (let [root-of-container? (partial value-of-match container?)
          build-loc (comp z/down z/vector-zip)
          childs (z/children loc)]
      (map build-loc (filter root-of-container? childs))))

; foward declaration for mutual recursive functions.
(declare empty-container?)
(defn empty-section?
  "returns logical true if the loc of the given section is empty. The
  location reveived is the location of something that looks like this
  [:section [:title 'title'] [:text 'text']]. This function is mutual 
  recursive with empty-container?"
  [loc]
  (let [text-nil (no-section-text? loc)
        container-locs (container-node-locs  loc)]
    (and text-nil (every? identity (map empty-container? container-locs)))))
    
(defn empty-container?
  "returns logical true if the container only contains empty section. This
  function is mutual recursive with empty-section?."
  ; loc is the location of some container (:sections, subs1 etc.)
  [loc]
  ; create root locations for every section in the sections container.
  (let [section-locs (map z/vector-zip (z/rights loc))]
    (every? identity (map empty-section? section-locs))))

(defn del-empty-sections
  "remove empty sections container. The definition of emptyness is this 
  function precisely."
  [syntax-tree]
  (loop [cur (z/vector-zip syntax-tree)]
    (let [node (z/node cur)]
      (cond 
        (z/end? cur) (z/root cur)
        (and (container? node) (empty-container? cur)) (recur 
                                                         (z/remove (z/up cur)))
        (and (section? node) (empty-section? (z/up cur))) (recur
                                                            (z/remove
                                                              (z/up cur)))
        :else (recur (z/next cur))))))
; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
