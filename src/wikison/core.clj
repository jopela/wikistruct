(ns wikison.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.tools.cli :as c]
            [clojure.string :as string]
            [clojure.set :as cset]
            [clojure.data.json :as json]
            [instaparse.core :as insta])
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
  (insta/parser
    "
    article  = (abstract |abstract section+)
    text     = #'[a-zA-Z0-9 \\.\\n]+'
    title    = #'[a-zA-Z0-9 \\.\\n]+'
    abstract = #'[a-zA-Z0-9 \\.\\n]+'
    section  = (h1|h1 text|h1 text sub1+|h1 sub1+)
    sub1     = (h2|h2 text|h2 text sub2+|h2 sub2+)
    sub2     = (h3|h3 text|h3 text sub3+|h4 sub3+)
    sub3     = (h4|h4 text|h4 text sub4+|h4 sub4+)
    sub4     = (h5|h5 text|h5 text sub5+|h5 sub5+)
    sub5     = (h6|h6 text)
    <h1>     = <'=='> title <'=='> <#'\\n'>
    <h2>     = <'==='> title <'==='> <#'\\n'> 
    <h3>     = <'===='> title <'===='> <#'\\n'> 
    <h4>     = <'====='> title <'====='> <#'\\n'> 
    <h5>     = <'======'> title <'======'> <#'\\n'> 
    <h6>     = <'======='> title <'======'> <#'\\n'> 
    "))

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

(defn raw-article-prop
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
  json result) from the raw-article-prop result " 
  [raw]
  (let [new-raw (cset/rename-keys raw
                                  {:fullurl :url
                                   :pagelanguage :lang})]
    (select-keys new-raw [:url :title :pageid :lang])))

(defn languages-extract
  "extract the languages from the raw-article-prop result "
  [raw]
  (let [raw-languages (raw :langlinks)]
    {:other-langs (vec (map :lang raw-languages))}))

(defn thumbnail-extract
  "extract the thumbnail from the raw-article-prop result "
  [raw]
  {:depiction (-> raw :thumbnail :source)})

(defn concat-symbols
  "apply a merging function using delim to keyword k within the parse tree
  result given by wiki-parser."
  [delim k & args]
    {k (string/join delim args)})

(def abstract-merger (partial concat-symbols"\n" :abstract))
(def text-merger (partial concat-symbols"\n" :text))
(def title-merger (partial concat-symbols" " :title))

; TODO: this implementation is not yet completed.
(defn text-extract
  ; this is not clear.
  "extract the abstract and sections from the raw-article-prop result. Does so
  by creating maps for section content and preserves hierarchy of original
  article."
  [raw]
  (let [text (raw :extract)
        parsed-tree (wiki-parser text)]
    (select-keys parsed-tree [:abstract :sections])))

(defn article 
  "return a json document built from the given url"
  [url]
  (let [raw-result (raw-article-prop url)
        simple (simple-prop-extract raw-result)
        lang (languages-extract raw-result)
        thumb (thumbnail-extract raw-result) ]
    (apply merge [simple lang thumb])))

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
    
    (let [articles (map raw-article-prop args)
          extracts (map :extract articles)]
      (println (string/join "\n\n\n*+*+*+*+*+*+*+*+*\n\n\n" extracts)))))

; for quick testing/prototyping in the repl
(def simple-test-10
  (slurp "./test/wikison/extracts/simple-test-10.txt"))

(def tree (wiki-parser simple-test-10))

