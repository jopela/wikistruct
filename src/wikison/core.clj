(ns wikison.core
  (:gen-class)
  (:require [clojure.tools.cli :as c]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.pprint :as p]
            [instaparse.core :as insta]
            [clojure.java.io :as io]
            [wikison.filters :as filters]
            [wikison.request :as request]
            [wikison.extract :as extract]
            [wikison.parse :as parse]))

; This grammar refuses to parse the '' article? why? must be a detail.
(def grammar-content 
  (-> "grammar.txt" io/resource io/input-stream slurp))

(def wiki-parser 
  (insta/parser grammar-content))

(defn text-eval 
  "evaluates the article syntax tree, which generates the text properties for
  a wiki article."
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
(defn text-parse
  "generate a parse tree of the article text from the raw result"
  [raw]
  (let [wiki-creole (str (raw :extract) "\n")]
    (wiki-parser wiki-creole)))

(defn text-extract
  "transform the wiki-creole text into a hash-map"
  [raw]
  ; appending a newline to the raw text is a workaround for an issue i cannot
  ; yet fix. it allows the use of the current grammar which is non-ambiguous.
  (let [parse-tree (text-parse raw)]
    (text-eval parse-tree)))

(defn article 
  "return a json document built from the given url"
  [user-agent url]
  (let [raw-result (request/raw-article user-agent url)
        simple (extract/simple-prop-extract raw-result)
        lang (extract/languages-extract raw-result)
        thumb (extract/thumbnail-extract raw-result) 
        text  (text-extract raw-result)]
    (apply merge [simple lang thumb text])))

; core 
;(defn article
;  "return a document that is a collection of information fetched from url"
;  ([filter-coll text-evaluator user-agent url]
;   true)
;  ([user-agent url]
;   (article nil nil user-agent url)))


(defn -main
  "json artcile from (media)wiki urls"
  [& args]
  (let [ [options args banner]
         (c/cli args
             ["-h" "--help" "print this help banner and exit" :flag true]
             ["-n" "--name" "the name that will be put in the request to the
                            media wiki API (typically yours)"]
             ["-u" "--user" "user-agent heder. Should include your mail"])]

    (when (options :help)
      (println banner)
      (System/exit 0))
    
    (when-not (options :user)
      (println "User agent is required! See --help for details")
      (System/exit 1))

    (let [user-agent (:user options)
          articles (map (partial article user-agent) args) ]
      (doseq [art articles]
        (p/pprint art)))))

