(ns wikison.api
  (:require [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [wikison.eval :as weval]
            [clojure.string :as string]
            [wikison.request :as request]
            [wikison.extract :as extract]
            [wikison.filters :as filters-func]
            [wikison.extract :as extract])
  (:import (java.net MalformedURLException)))

(timbre/refer-timbre)

; default values for command line arguments.
(def log-file-default "wikison.log")
(def filter-conf-default (-> "filter.conf" io/resource .getPath))

(defn load-config
  "loads config data from a file."
  [filename]
  (set (map string/lower-case (load-string (slurp filename)))))

(defn default-post-filters
  "Returns a list of filters that we use by default. ORDER IS IMPORTANT since
  filter application is ASSOCIATIVE but not COMMUTATIVE. Input is the set
  of removable section for the deletion of unwanted section from the wiki."
  [removable-set]
  (let [del-unwanted-section (partial filters-func/remove-sec-function removable-set)]
    [filters-func/del-voir
     filters-func/del-pronounciation 
     filters-func/del-about
     filters-func/del-empty-sections 
     del-unwanted-section]))

(defn error-report
  "generates a human readable error report of the result of the editorial
  content generation."
  [error-sources]
  (doseq [e error-sources]
    (error (str (e :error) "\n"))))

(def default-text-filters
  [
   filters-func/remove-single-word-line
   filters-func/remove-short-lines
   filters-func/remove-brackets
   filters-func/remove-brackets-all
   filters-func/remove-portail
   filters-func/remove-coordinates-text
   filters-func/remove-plan-text
   ])

(def default-eval-function
  weval/tree-eval-html-partial)

(defn article
  "Return a document based on information fetched from the given url.
  filters and eval-func MUST be found in the wikison.filters and wikison.eval
  namespace respectively."
  ([raw-text-filters post-filters eval-func user-agent url]
   (let [raw-result (try 
                      (request/raw-article user-agent url) 
                      (catch 
                        MalformedURLException 
                        e {:error (str url " is a malformed url")}))]
     (if (raw-result :error)
      raw-result 
       (let [ simple-properties (extract/simple-prop-extract raw-result)
              lang (extract/languages-extract raw-result)
              thumb (extract/thumbnail-extract raw-result) 
              text (try (extract/text-extract raw-text-filters post-filters eval-func raw-result) (catch Exception e nil))]
         (if (nil? text)
          {:error (str "wiki-creole parsing error for " url)}
          (apply merge [simple-properties lang thumb text]))))))

  ([user-agent url]
   (let [filter-conf (load-config filter-conf-default)]
     (article default-text-filters
              (default-post-filters filter-conf)
              default-eval-function
              user-agent
              url))))

(defn error?
  "returns logical true if the result returned by article is an error
  dictionnary. Return true otherwise."
  [article]
  (contains? article :error))

(defn depiction-url
  "returns the depiction url of the given wikipedia url."
  [user-agent url]
  (request/depiction-raw user-agent url))

(def not-error? (complement error?))

