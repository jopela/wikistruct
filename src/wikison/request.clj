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
  (if-let [path (. (URL. url) getFile) ]
    ; URLDecoder/decode so that url-encoded strings can be used to derive 
    ; real page titles.
   (URLDecoder/decode (last (string/split path #"/")) "UTF-8")
   nil))
 
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

(defn url-pageid
  "takes a url and returns a a pageid if it exists. Returns nil if not present"
  [url]
  (letfn [(split-first [x] (first (string/split x #"=")))
          (split-second [x] (second (string/split x #"=")))]
    (let [parsed (URL. url)]
      (if-let [query (. parsed getQuery)]
        (let [components (string/split query #"&")
              dictionary (zipmap (map split-first components) 
                                 (map split-second components))]
          (dictionary "curid"))
      nil))))

(defn raw-article-dispatch
  "takes a url and returns :pageids or :titles if it contains a pageid or a 
  title. Otherwise return nil. User-agent argument is included to make 
  signature identical to the 
  calling function"
  [user-agent url]
  (let [not-nil? (complement nil?)]
    (cond
      (not-nil? (url-pageid url)) :pageids
      (not-nil? (article-title url)) :titles
      :else nil)))

(defn raw-article-query
  "function template for fetching an article with the mediawiki API"
  [user-agent handle-fn url]
  (let [handle (handle-fn url)
        params (merge {"inprop" "url"
                       "prop"   "info|pageprops|extracts|langlinks|pageimages"
                       "explaintext" ""
                       "piprop" "thumbnail"
                       "pithumbsize" 9999
                       "lllimit" 150 } handle)]
    (mediawiki-req user-agent url params)))

(defn query-handle
  "takes a url and returns an handle query map"
  [property handle-extract url]
  {property (handle-extract url)})

(def title-handle (partial query-handle "titles" article-title))
(def pageid-handle (partial query-handle "pageids" url-pageid))

(defmulti raw-article raw-article-dispatch)

(defmethod raw-article :pageids [user-agent url] 
  (raw-article-query user-agent pageid-handle url))

(defmethod raw-article :titles [user-agent url]
  (raw-article-query user-agent title-handle url))

(defmethod raw-article :default [user-agent url]
  {:error (str "could not figure out how to issue a media wiki API call to "
               url)})

(defn depiction-request
  "fowards a request to the media-wiki api for a depiction."
  [user-agent handle url]
  (let [params (merge {"prop" "pageimages"
                       "piprop" "thumbnail"
                       "pithumbsize" 99999
                       "pilimit" 1} handle)
        response (mediawiki-req user-agent url params)]
    (mediawiki-req user-agent url params)))

(defmulti depiction-raw raw-article-dispatch)

(defmethod depiction-raw :pageids [user-agent url]
  (depiction-request user-agent (pageid-handle url) url))

(defmethod depiction-raw :titles [user-agent url]
  (depiction-request user-agent (title-handle url) url))


