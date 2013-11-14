(ns wikison.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.tools.cli :as c]
            [clojure.string :as string]
            [clojure.set :as cset])
  (:import (java.net URL)))
; TODO: ++ the limits on received languages links.
(defn -main
  "json artcile from (media)wiki urls"
  [& args]
  (let [ [options args banner]
         (c/cli args
             ["-h" "--help" "print this help banner and exit" :flag true]) ]

    (when (options :help)
      (println banner)
      (System/exit 0))
    
    (println "No options given")))

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
    (last (string/split path #"/"))))

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
                "pithumbsize" 9999}]

    (mediawiki-req url params)))

(defn simple-prop-extract
  "Extract the simple properties (ones that directly map to a property in a 
  wanted result) from the raw-article-prop result " 
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

(defn article 
  "return a json document built from the given url"
  [url]
  (let [raw-result (raw-article-prop url)
        simple (simple-prop-extract raw-result)
        lang (languages-extract raw-result)
        thumb (thumbnail-extract raw-result) ]
    (apply merge [simple lang thumb])))

(def mother-url "https://en.wikipedia.org/wiki/Whistler's_Mother")
(def mother (raw-article-prop mother-url))

