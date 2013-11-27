(ns wikison.request
  "Code that queries the MediaWiki API."
  (:require [clojure.core.match :as match]
            [clojure.string :as string]
            [clj-http.client :as client])
  (:import (java.net URL URLEncoder URLDecoder))) 

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
            
