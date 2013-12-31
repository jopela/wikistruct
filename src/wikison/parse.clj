(ns wikison.parse
  (:require [clojure.string :as string]
            [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.pprint :as p]))

(defn merge-sentence
  "merge sentences into text nodes. We consider this as being part of
  parsing. Perhaps the grammar could be modified so that this step becomes
  unnecessary."
  [syntax-tree]
  (letfn [(fusion [kw & args] [kw (apply str args)])]
    (insta/transform {:sentence str
                      :text (partial fusion :text)
                      :abstract (partial fusion :abstract)}
                     syntax-tree)))

; This grammar refuses to parse the '' article? why? must be a detail.
(def grammar-content 
  (-> "grammar.txt" io/resource io/input-stream slurp))

(def wiki-parser 
  (insta/parser grammar-content))

(defn creole-parse
  "transform some  wiki creole into an abstract syntax tree"
  [creole]
  (let [parse-result (-> creole wiki-parser)]
    (cond 
      (insta/failure? parse-result) nil
      :else (-> creole wiki-parser merge-sentence))))

(defn wiki-creole-parse
  "generate a parse tree of the article text from the raw result"
  [raw]
  (let [wiki-creole (str (raw :extract) "\n")]
    (creole-parse wiki-creole)))

