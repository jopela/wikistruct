(ns wikison.parse
  (:require [clojure.string :as string]
            [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.pprint :as p]))

; This grammar refuses to parse the '' article? why? must be a detail.
(def grammar-content 
  (-> "grammar.txt" io/resource io/input-stream slurp))

(def wiki-parser 
  (insta/parser grammar-content))

(defn wiki-creole-parse
  "generate a parse tree of the article text from the raw result"
  [creole]
  (let [wiki-creole (str (creole :extract) "\n")]
    (wiki-parser wiki-creole)))
