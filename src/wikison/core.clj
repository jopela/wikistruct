(ns wikison.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.tools.cli :as c]
            [clojure.string :as string]
            [clojure.set :as cset]
            [clojure.data.json :as json]
            [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.pprint :as p]
            [wikison.filters :as filters])

  (:import (java.net URL URLEncoder URLDecoder)))

; TODO: docstring quality is overall poor. See high-ranking clojure projects
; (ring?) for inspiration on how to write better docstring.

; This grammar refuses to parse the '' article? why? must be a detail.
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
  [user-agent url params]
  (let [req-url (api-url url)
        req-params (merge {"format" "json"
                           "action" "query"
                           "ppprop" "disambiguation"} params)
        req-format (-> "format" req-params  keyword )
        resp-dic   (-> (client/get req-url {:query-params req-params
                                            :as req-format
                                            :headers 
                                            {"User-Agent" user-agent}})
                       :body
                       :query
                       :pages)]
    (-> resp-dic keys first resp-dic)))

(defn raw-article
  "retrieve article properties that will go into the json article. This is
  the raw result from the wiki api"
  [user-agent url]
  (let [title  (article-title url)
        params {"titles" title
                "inprop" "url"
                "prop"   "info|pageprops|extracts|langlinks|pageimages"
                "explaintext" ""
                "piprop" "thumbnail"
                "pithumbsize" 9999
                "lllimit" 150
                }]
    (mediawiki-req user-agent url params)))

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
  (let [raw-result (raw-article user-agent url)
        simple (simple-prop-extract raw-result)
        lang (languages-extract raw-result)
        thumb (thumbnail-extract raw-result) 
        text  (text-extract raw-result)]
    (apply merge [simple lang thumb text])))

(defn article
  "return a document that is a collection of information fetched from url"
  ([filter-coll text-evaluator user-agent url]
   true)
  ([user-agent url]
   (article nil nil user-agent url)))


; Main entry point
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

