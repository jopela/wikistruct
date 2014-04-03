(ns wikison.filters
  "Collection of filter functions (and associated helper functions) applied 
  before the evaluation of the article tree into a concrete representation 
  (e.g: html  json etc.). A filter function could befor example  a function 
  to remove unwanted sections or to remove certain words from the text."
  (:require [clojure.core.match :as match]
            [clojure.string :as string]
            [instaparse.core :as insta]
            [clojure.zip :as z]))

(defn value-of-match
  "takes the given node and tries to match it for a [k v] pattern. if it 
  matches and k is in the kws set, return the value of v. Else, return nil."
  [kws node]
  (match/match node
    [k v & args] (if (kws k) v nil)
    :else nil))

(defn match-any?
  "Returns logical true if (-> txt trim lower-case) belongs to rs"
  [rs txt]
  (let [cmp-txt (-> txt string/trim string/lower-case)]
    (rs cmp-txt)))

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

(defn remove-sec-function
  "remove section from syntax-tree that can be found in the given title-set."
  [title-set syntax-tree]
  (let [match-removable? (partial match-any? title-set)
        del-unwanted-sec (partial del-sec-with-title match-removable?)]
    (del-unwanted-sec syntax-tree)))

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

(defn remove-nested-parens
  "remove nested parenthesis from the text"
  [text]
  (try
    (string/replace text #" ?(\((?:\(.*?\)|[^\(])*?\))" "")
    (catch StackOverflowError e text)))

(defn remove-foward-slash
  "remove all the text in between 2 foward slash"
  [text]
  (string/replace text #"/.*/" ""))

(defn remove-space-comma
  "remove space before a comma"
  [text]
  (string/replace text #" ," ","))

(defn remove-2-spaces
  "remove 2 consecutive spaces from string"
  [text]
  (string/replace text #"  " " "))

(defn remove-pronounciation-text
  "remove text pronounciation from a piece of text"
  [text]
  (-> text
      remove-nested-parens
      remove-foward-slash
      remove-2-spaces
      remove-space-comma))

(defn remove-about-text
  "remove 'this article is about' kind of sentences."
  [text]
  (string/replace text #"(?i)this article is about.*for other.*\n+" ""))

(defn remove-contains-word
  "if the text contains 'voir', return the empty string"
  [word text]
  (let [pattern (format "(?i).*%s.*\n?" word)
        regex (re-pattern pattern)]
    (string/replace text regex ""))) 

(defn abstract-word-fn
  [word text]
  (let [lines (string/split-lines text)
        x (first lines)
        xs (rest lines)
        clean-fn (partial remove-contains-word word)]
    [:abstract (string/replace 
                 (string/join "\n" (cons (clean-fn x) xs))
                 #"^\n" 
                 "")]))

(defn del-abstract-word
  "remove first sentence of abstract that contain voir."
  [word syntax-tree]
  (insta/transform
    {:abstract (partial abstract-word-fn word)}
    syntax-tree))

(def del-voir (partial del-abstract-word "voir"))

(defn del-pronounciation
  "remove pronounciation text from the first sentence of the abstract section."
  [syntax-tree]
  (insta/transform 
    {:abstract (fn [x] [:abstract (remove-pronounciation-text x)])} 
    syntax-tree))

(defn del-about
  "remove 'This article is about' kind of phrases from article abstract
  sections."
  [syntax-tree]
  (insta/transform
    {:abstract (fn [x] [:abstract (remove-about-text x)])}
    syntax-tree))

; ~~~~~~~~~~ text filters.
(defn remove-brackets
  "remove square brackets from text when they either contain 'Citation needed'
  or numbers."
  [text]
  (string/replace text #"(?i)\[Citation needed\]|\[[0-9]+\]" ""))

(defn remove-brackets-all
  "remove all square brackets from the text."
  [text]
  (string/replace text #"(?s)\s?\[.*\]\s?" " "))

(defn remove-portail
  "remove sentences that have the words Portail in them."
  [text]
  (string/replace text #"\s?Portail .*?(\.|\n|$)" ""))

(defn remove-coordinates-text
  "remove the coordinates that appear at the beginning of a sentence."
  [text]
  (string/replace text #"-?[0-9]{1,3}\.[0-9]+,?\s?-?[0-9]{1,3}\.[0-9]+\s?" ""))

(defn remove-short-lines
  "remove lines that contain less then 4 characters."
  [text]
  (string/replace (string/replace text #"(?m)^.{1,4}$" "") #"^\n" ""))

(defn remove-plan-text
  "remove sentences that begins by Plan officiel."
  [text]
  (string/replace text #"Plan officiel .*" ""))

(defn remove-single-word-line
  "remove lines that only contain a single word"
  [text]
  (string/replace text #"^[^\s]+\n" "" ))

; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

