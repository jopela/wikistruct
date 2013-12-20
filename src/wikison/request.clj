(ns wikison.request
  "Code that queries the MediaWiki API."
  (:require [clojure.core.match :as match]
            [clojure.string :as string]
            [clj-http.client :as client]
            [clojure.data.json :as json])
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
 
(defn- response-dic
  "return the value of the pages dictionary found in the wiki API response"
  [http-correct-response]
  (-> http-correct-response 
      :body 
      (json/read-str :key-fn keyword) 
      :query 
      :pages))

(defn- first-page
  "return the value of the first key inside the pages dictionary of
  the result from the mediawiki api."
  [http-correct-response]
  (let [response (response-dic http-correct-response)
        first-key (-> response keys first)]
    (response first-key)))

(defn- page-missing?
  "returns true if the json-content contains indication that the mediawiki
  page returned is missing."
  [json-content]
  (contains? json-content :missing))

(defn- request-content-type
  "extract the content type from the request object"
  [raw-response]
  ((raw-response :headers) "content-type"))

(defn- wiki-response
  "treats the mediawiki API response after content presence and http error 
  codes have been checked"
  [url http-correct-resp]
  (let [content-type (request-content-type http-correct-resp)
        expected-type "json"]
    (if (not (.contains content-type expected-type))
      {:error (str "asked " 
                   url 
                   " for json but she returned " 
                   content-type 
                   ". Will not parse!")}
      (let [json-content (first-page http-correct-resp)]
        (cond 
          (page-missing? 
            json-content) {:error (str 
                                    "page for " 
                                    url 
                                    " does not exist on the queried wiki")}
          :else json-content)))))

(defn mediawiki-req
  "Fowards a request to the (media)wiki api"
  [user-agent url params]
  (let [error-codes (set (range 400 501))
        req-url (api-url url)
        req-params (merge {"format" "json"
                           "action" "query"
                           "ppprop" "disambiguation"} params)
        resp-dic   (client/get req-url {:query-params req-params
                                            :headers {"User-Agent" user-agent}
                                            :throw-exceptions false
                                            :ignore-unknown-host? true})]
    ; error handling.
    (cond
      (nil? resp-dic) {:error (str "hostname for "
                                   url
                                   " could not be resolved")}
      (-> resp-dic 
          :status 
          error-codes) {:error (str "query for "
                                    url 
                                    " returned http error code "
                                    (-> resp-dic :status))}


      :else (wiki-response url resp-dic))))

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
            
