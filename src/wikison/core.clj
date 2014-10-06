(ns wikison.core
  (:gen-class)
  (:require [clojure.tools.cli :as c]
            [clojure.pprint :as p]
            [wikison.request :as request]
            [wikison.eval :as weval]
            [wikison.extract :as extract]
            [wikison.api :as api]
            [wikison.parse :as parse]
            [clojure.java.io :as io]
            [clojure.zip :as z]
            [clojure.data.json :as json]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [taoensso.timbre :as timbre])
  (:import (java.net MalformedURLException)))

(timbre/refer-timbre)

(defn -main
  "json article from (media)wiki urls"
  [& args]
  (let [ {:keys [errors options arguments summary]}
         (c/parse-opts args
             [["-h" "--help" "print this help banner and exit"]
             ["-u" "--user USER" "user-agent header. Should include your mail"]
             ["-a" "--article" "extract only the article part and print it to stdout. Uses the partial evaluator by default"]
             ["-m" "--markup-html" "return a (totally) rendered html version of the article part"]
             ["-l" "--log PATH" "path to the log file." :default "wikison.log" ]
             ["-d" "--depiction" "extract the depiction of given resources"]
             ["-c" "--config PATH" "the path to the configuration file containing the removable section map" :default api/filter-conf-default]])]

    (when (options :help)
      (println summary)
      (System/exit 0))
    
    (when-not (options :user)
      (println "User agent is required! See --help for details")
      (System/exit 1))

    (let [user-agent (:user options)
          filter-config (api/load-config api/filter-conf-default) 
          log-file-path (:log options)
          default-filters-post (api/default-post-filters filter-config)]

      (timbre/set-config! [:appenders :spit :enabled?] true)
      (timbre/set-config! [:shared-appender-config :spit-filename] "wikison.log")
      (timbre/set-config! [:appenders :standard-out :enabled?] false)
      (cond 
       (options :article) (let [articles (map 
                                           (partial api/article 
                                                    api/default-text-filters 
                                                    default-filters-post 
                                                    api/default-eval-function 
                                                    user-agent) 
                                           args)]
        (doseq [art articles]
         (println (art :article))))
       (options :markup-html) (let [articles (map 
                                          (fn [x] (api/article 
                                                    api/default-text-filters
                                                    default-filters-post
                                                    weval/tree-eval-html
                                                    user-agent
                                                    x))
                                          args)
                                    errors (filter api/error? articles)
                                    valid (filter api/not-error? articles)]
           (api/error-report errors)
           (p/pprint (vec (map :article valid)))
           (System/exit 0))
       (options :depiction) (do (doseq [d (map #(api/depiction-url user-agent %) args)]
                              (if (api/error? d) 
                                (do (error (str (d :error) "\n")) (println nil))
                                (p/pprint (:depiction (extract/thumbnail-extract d)))))
                                (System/exit 0))

       :else (do (p/pprint (map (partial api/article api/default-text-filters default-filters-post api/default-eval-function user-agent) args)) 
                 (System/exit 0))))))
