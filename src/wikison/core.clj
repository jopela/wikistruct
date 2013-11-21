(ns wikison.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.tools.cli :as c]
            [clojure.string :as string]
            [clojure.set :as cset]
            [clojure.data.json :as json]
            [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.pprint :as p])
  (:import (java.net URL URLEncoder URLDecoder)))

; TODO: docstring quality is overall poor. See high-ranking clojure projects
; (ring?) for inspiration on how to write better docstring.
; TODO: must support overriding user-agent header.
; TODO: must cache the media-wiki request .
; TODO: must transform the parse tree into the dictionary expected as result.
; (IN PROGRESS)
; TODO: must add unicode character support (I dont know why \p{L} will not 
; match unicode in given text).
; TODO: must implement robust test case for a wide variety of articles.
; TODO: must add test case for article in all of these supported languages:

; This is a context-free grammar that parses a subset of the wiki creole 1.0
; syntax. Used to break down our the article extracts into it's component while
; keeping the hierarchy amongst sections and subsection. Please refer
; to the wonderful documentation of instaparse at:
; https://github.com/Engelberg/instaparse for more information.

; this grammar refuses to parse the '' article? why?
(def wiki-parser 
  (insta/parser "./resources/grammar.txt"))

(defn api-url
  "return a (media)wiki api url based on the url given as argument"
  [url]
  (let [parsed (URL. url)
        proto (. parsed getProtocol)
        host  (. parsed getHost)
        file  "/w/api.php"]
    (. (URL. proto host file) toString)))

(defn article-title
  "returns the title of the page associated with a url"
  [url]
  (let [path (. (URL. url) getFile) ]
    ; URLDecoder/decode so that url-encoded strings can be used to derive 
    ; real page titles.
   (URLDecoder/decode (last (string/split path #"/")) "UTF-8")))

(defn mediawiki-req
  "Fowards a request to the (media)wiki api"
  [url params]
  ; TODO: put the very frequently used, rarely overridden params here
  (let [req-url (api-url url)
        req-params (merge {"format" "json"
                           "action" "query"
                           "ppprop" "disambiguation"} params)
        req-format (-> "format" req-params  keyword )
        resp-dic   (-> (client/get req-url {:query-params req-params
                                            :as req-format})
                       :body
                       :query
                       :pages)]
    (-> resp-dic keys first resp-dic)))

(defn raw-article

  "retrieve article properties that will go into the json article. This is
  the raw result from the wiki api"
  [url]
  (let [title  (article-title url)
        params {"titles" title
                "inprop" "url"
                "prop"   "info|pageprops|extracts|langlinks|pageimages"
                "explaintext" ""
                "piprop" "thumbnail"
                "pithumbsize" 9999
                "lllimit" 150
                }]
    (mediawiki-req url params)))

(defn simple-prop-extract
  "Extract the simple properties (ones that directly map to a property in the
  json result) from the  raw-article
  result" 
  [raw]
  (let [new-raw (cset/rename-keys raw
                                  {:fullurl :url
                                   :pagelanguage :lang})]
    (select-keys new-raw [:url :title :pageid :lang])))

(defn languages-extract
  "extract the languages from the  raw-article
  result"
  [raw]
  (let [raw-languages (raw :langlinks)]
    {:other-langs (vec (map :lang raw-languages))}))

(defn thumbnail-extract
  "extract the thumbnail from the  raw-article
  result"
  [raw]
  {:depiction (-> raw :thumbnail :source)})

(defn text-eval 
  "evaluates the article syntax tree, which generates the text properties for
  a wiki article."
  [syntax-tree]
  (letfn [ (title [s] {:title s})
           (text  [s] {:text  s})
           (abstract [s] {:abstract s})
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
                      :article merge}
                     syntax-tree)))

(defn text-extract
  "transform the wiki-creole text into a hash-map"
  [raw]
  (let [wiki-creole (raw :extract)
        parse-tree (wiki-parser wiki-creole)]
    (text-eval parse-tree)))

(defn article 
  "return a json document built from the given url"
  [url]
  (let [raw-result (raw-article url)
        simple (simple-prop-extract raw-result)
        lang (languages-extract raw-result)
        thumb (thumbnail-extract raw-result) 
        text  (text-extract raw-result)]
    (apply merge [simple lang thumb text])))

; Main entry point
(defn -main
  "json artcile from (media)wiki urls"
  [& args]
  (let [ [options args banner]
         (c/cli args
             ["-h" "--help" "print this help banner and exit" :flag true]
             ["-n" "--name" "the name that will be put in the request to the
                            media wiki API (typically yours)"]
             ["-e" "--email" "your email address"])]

    (when (options :help)
      (println banner)
      (System/exit 0))
    
    (let [articles (map article args) ]
      (doseq [art articles]
        (p/pprint art)))))

(def simple-test-1
  (slurp "./test/wikison/extracts/simple-test-1.txt"))
(def tree-1 (wiki-parser simple-test-1))

(def simple-test-2
  (slurp "./test/wikison/extracts/simple-test-2.txt"))
(def tree-2 (wiki-parser simple-test-2))

(def russian-1
  (slurp "./test/wikison/extracts/ru-test-1.txt"))
(def ru-tree-1 (wiki-parser russian-1))
