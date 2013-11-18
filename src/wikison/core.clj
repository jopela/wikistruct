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

; This is a context-free grammar that parses a subset of the wiki creole 1.0
; syntax. Used to break down our the article extracts into it's component while
; keeping the hierarchy amongst sections and subsection.
(def wiki-parser 
  (insta/parser
    "article  = epsilon
     article  = abstract (sep section)*
     abstract = line+
     sep      = #'\\n{2,2}'
     section  = h1 line* (sep sub1)*
     sub1     = h2 line* (sep sub2)*
     sub2     = h3 line*  
     h1       = '== ' title  ' ==' #'\\n'
     h2       = '=== ' title ' ===' #'\\n'
     h3       = '==== ' title ' ====' #'\\n'
     title    =  name (' ' name)*
     name     = #'[a-zA-Z]+'
     line     = #'[a-zA-Z \\.]*\\n'"))

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

(defn sections-extract
  ; this is not clear.
  "extract the sections from the raw-article-prop result. Does so
  by creating maps for section content and preserves hierarchy of original
  article."
  [raw]
  (let [text (raw :extract)]
    text))

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
             ["-h" "--help" "print this help banner and exit" :flag true]) ]

    (when (options :help)
      (println banner)
      (System/exit 0))
    
    (let [articles (map raw-article-prop args)
          extracts (map :extract articles)]
      (println (string/join "\n\n\n*+*+*+*+*+*+*+*+*\n\n\n" extracts)))))

(def simple-test-6 
  (slurp "/root/dev/wikison/test/wikison/extracts/simple-test-6.txt"))

(def simple-test-7 
  (slurp "/root/dev/wikison/test/wikison/extracts/simple-test-7.txt"))

(def simple-test-8
  (slurp "/root/dev/wikison/test/wikison/extracts/simple-test-8.txt"))


