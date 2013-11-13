(ns wikison.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.tools.cli :as c])
  (:import (java.net URL)))

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
  "Our_Lady_of_the_Don")


(defn mediawiki-req
  "Fowards a request to the (media)wiki api"
  [url params]
  ; TODO: put the very frequently used, rarely overridden params here
  (let [req-url (api-url url)
        req-params (merge {"format" "json" "action" "query"} params)
        req-format (-> req-params "format" keyword ) ]
    (client/get req-url {:query-params req-params :as req-format})))

;(defn gen-art-prop
;  "retrieve general json article properties"
;  [url]
;  (let [params {"action" "query"

(defn article 
  "return a json document built from the given url"
  [url]
  {:url   "http://en.wikipedia.org/wiki/Whistler's_Mother"
   :title "Whistler's_Mother"})


