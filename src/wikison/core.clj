(ns wikison.core
  (:gen-class)
  (:require [clojure.tools.cli :as c]
            [clojure.pprint :as p]
            [wikison.filters :as filters-func]
            [wikison.request :as request]
            [wikison.eval :as weval]
            [wikison.extract :as extract]
            [wikison.parse :as parse]
            [clojure.zip :as z]
            [clojure.data.json :as json]
            [clojure.pprint :as pprint]
            [taoensso.timbre :as timbre])
  (:import (java.net MalformedURLException)))

(timbre/refer-timbre)

(defn error-report
  "generates a human readable error report of the result of the editorial
  content generation."
  [error-sources]
  (doseq [e error-sources]
    (error (str (e :error) "\n"))))

(def default-post-filters
  [filters-func/del-pronounciation 
   filters-func/del-empty-sections 
   filters-func/del-unwanted-sec])

(def default-text-filters
  [filters-func/remove-brackets
   filters-func/remove-portail])

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

  ([post-filters eval-func user-agent url]
   (article default-text-filters
            post-filters
            eval-func
            user-agent
            url))

  ([eval-func user-agent url]
   (article default-text-filters
            default-post-filters
            eval-func
            user-agent 
            url))

  ([user-agent url]
   (article default-text-filters
            default-post-filters
            default-eval-function
            user-agent 
            url)))

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
  (let [ [options args banner]
         (c/cli args
             ["-h" "--help" "print this help banner and exit" :flag true]
             ["-u" "--user" "user-agent header. Should include your mail"]
             ["-a" "--article" "extract only the article part and print it to
                               stdout. Uses the partial evaluator by default" 
              :flag true]
             ["-m" "--markup-html" "return a (totally) rendered html version of 
                                   the article part" :flag true]
             ["-d" "--depiction" "extract the depiction of given resources" :flag true])]
    (when (options :help)
      (println banner)
      (System/exit 0))
    
    (when-not (options :user)
      (println "User agent is required! See --help for details")
      (System/exit 1))

    (let [user-agent (:user options)]
      ; logger configuration
      (timbre/set-config! [:appenders :spit :enabled?] true)
      (timbre/set-config! [:shared-appender-config :spit-filename] 
                          "/var/log/wikison.log")
      (timbre/set-config! [:appenders :standard-out :enabled?] false)
      (cond 
       (options :article) (let [articles (map 
                                           (partial article user-agent) args)]
        (doseq [art articles]
         (println (art :article))))
       (options :markup-html) (let [articles (map 
                                          (fn [x] (article 
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

       :else (do (p/pprint (map (partial article user-agent) args)) 
                 (System/exit 0))))))


(def super-url "http://en.wikipedia.org/wiki/index.php?curid=18958249")

(:extract (request/raw-article "jopela" super-url))


