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
 
(defn- response-dic
  "return the value of the pages dictionary found in the wiki API response"
  [raw-response]
  (-> raw-response :body :query :pages))

(defn- first-page
  "return the value of the first key inside the pages dictionary of
  the result from the mediawiki api."
  [raw-response]
  (let [response (response-dic raw-response)
        first-key (-> response keys first)]
    (response first-key)))

(defn- page-id
  "extract the page id out of the raw response from the wikimedia api"
  [raw-response]
  (let [resp-dictionary (-> raw-response :body :query :pages)]
    (-> resp-dictionary keys first name (Integer/parseInt))))

(defn mediawiki-req
  "Fowards a request to the (media)wiki api"
  [user-agent url params]
  (let [error-codes (set (range 400 501))
        req-url (api-url url)
        req-params (merge {"format" "json"
                           "action" "query"
                           "ppprop" "disambiguation"} params)
        req-format (-> "format" req-params  keyword )
        resp-dic   (client/get req-url {:query-params req-params
                                            :as req-format
                                            :headers 
                                            {"User-Agent" user-agent}
                                            :throw-exceptions false
                                            :ignore-unknown-host? true})]
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

      (-> resp-dic page-id neg?) {:error (str "page for "
                                              url 
                                              " does not exist "
                                              "on the queried wiki")}
      

      :else (first-page resp-dic))))

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
            
