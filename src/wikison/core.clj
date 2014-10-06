(ns wikison.core
  (:gen-class)
  (:require [clojure.tools.cli :as c]
            [clojure.pprint :as p]
            [wikison.filters :as filters-func]
            [wikison.request :as request]
            [wikison.eval :as weval]
            [wikison.extract :as extract]
            [wikison.parse :as parse]
            [clojure.java.io :as io]
            [clojure.zip :as z]
            [clojure.data.json :as json]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [taoensso.timbre :as timbre])
  (:import (java.net MalformedURLException)))

(timbre/refer-timbre)

; default values for command line arguments.
(def log-file-default "/var/log/wikison.log")
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
  "return a document based on information fetched from the given url.
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

  ;other module uses a function call of this signature. In that case, assume default filters built from config file found at /etc/wikison.d/filter.conf.
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

(defn -main
  "json article from (media)wiki urls"
  [& args]
  (let [ {:keys [errors options arguments summary]}
         (c/parse-opts args
             [["-h" "--help" "print this help banner and exit"]
             ["-u" "--user USER" "user-agent header. Should include your mail"]
             ["-a" "--article" "extract only the article part and print it to stdout. Uses the partial evaluator by default"]
             ["-m" "--markup-html" "return a (totally) rendered html version of the article part"]
             ["-l" "--log PATH" "path to the log file." :default "/var/log/wikison.log" ]
             ["-d" "--depiction" "extract the depiction of given resources"]
             ["-c" "--config PATH" "the path to the configuration file containing the removable section map" :default filter-conf-default]])]

    (when (options :help)
      (println summary)
      (System/exit 0))
    
    (when-not (options :user)
      (println "User agent is required! See --help for details")
      (System/exit 1))

    (let [user-agent (:user options)
          filter-config (load-config filter-conf-default) 
          log-file-path (:log options)
          default-filters-post (default-post-filters filter-config)]

      (timbre/set-config! [:appenders :spit :enabled?] true)
      (timbre/set-config! [:shared-appender-config :spit-filename] "/var/log/wikison.log")
      (timbre/set-config! [:appenders :standard-out :enabled?] false)
      (cond 
       (options :article) (let [articles (map 
                                           (partial article 
                                                    default-text-filters 
                                                    default-filters-post 
                                                    default-eval-function 
                                                    user-agent) 
                                           args)]
        (doseq [art articles]
         (println (art :article))))
       (options :markup-html) (let [articles (map 
                                          (fn [x] (article 
                                                    default-text-filters
                                                    default-filters-post
                                                    weval/tree-eval-html
                                                    user-agent
                                                    x))
                                          args)
                                    errors (filter error? articles)
                                    valid (filter not-error? articles)]
           (error-report errors)
           (p/pprint (vec (map :article valid)))
           (System/exit 0))
       (options :depiction) (do (doseq [d (map #(depiction-url user-agent %) args)]
                              (if (error? d) 
                                (do (error (str (d :error) "\n")) (println nil))
                                (p/pprint (:depiction (extract/thumbnail-extract d)))))
                                (System/exit 0))

       :else (do (p/pprint (map (partial article default-text-filters default-filters-post default-eval-function user-agent) args)) 
                 (System/exit 0))))))
