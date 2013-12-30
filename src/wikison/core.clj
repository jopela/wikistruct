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
            [taoensso.timbre :as timbre])
  (:import (java.net MalformedURLException)))

(timbre/refer-timbre)

(defn error-report
  "generates a human readable error report of the result of the editorial
  content generation."
  [error-sources]
  (doseq [e error-sources]
    (error (str (e :error) "\n"))))

(def default-filters
  [filters-func/del-empty-sections filters-func/del-unwanted-sec])

(defn article
  "return a document based on information fetched from the given url.
  filters and eval-func MUST be found in the wikison.filters and  wikison.eval
  namespace respectively."

  ([filters eval-func user-agent url]
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
              text  (extract/text-extract filters eval-func raw-result)]
         (if (nil? text)
          {:error (str "wiki-creole parsing error for " url)}
          (apply merge [simple-properties lang thumb text]))))))

  ([eval-func user-agent url]
   (article default-filters
            eval-func
            user-agent url))

  ([user-agent url]
   (article default-filters
            weval/tree-eval-html-partial
            user-agent url)))

(defn error?
  "returns logical true if the result returned by article is an error
  dictionnary. Return true otherwise."
  [article]
  (contains? article :error))

(def not-error? (complement error?))

(defn -main
  "json article from (media)wiki urls"
  [& args]
  (let [ [options args banner]
         (c/cli args
             ["-h" "--help" "print this help banner and exit" :flag true]
             ["-u" "--user" "user-agent heder. Should include your mail"]
             ["-a" "--article" "extract only the article part and print it to
                               stdout. Uses the partial evaluator by default" 
              :flag true]
             ["-m" "--markup-html" "return a (totally) rendered html version of 
                                   the article part" :flag true])]
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
       :else (do (p/pprint (map (partial article user-agent) args)) 
                 (System/exit 0))))))

; ~~~ emergency bug test definitions
;(def user-agent "wikison 0.1.1 (jonathan.pelletier1@gmail.com)")
;(def url1 "http://ru.wikivoyage.org/wiki/Анкара")
;(def text1 (request/raw-article user-agent url1))
;(def tree1 (parse/wiki-creole-parse text1))
;
;(def url2 "http://en.wikipedia.org/wiki/S-expression")
;(def text2 (request/raw-article user-agent url2))
;(def tree2 (parse/wiki-creole-parse text2))
;
;(def url3 "http://arz.wikipedia.org/wiki/انقره")
;(def text3 (request/raw-article user-agent url3))
;(def tree3 (parse/wiki-creole-parse text3))
;
;(def art3 (article user-agent url3))
