(ns wikison.core
  (:gen-class)
  (:require [clojure.tools.cli :as c]
            [clojure.pprint :as p]
            [wikison.filters :as filters]
            [wikison.request :as request]
            [wikison.extract :as extract]))

(defn article 
  "return a json document built from the given url"
  [user-agent url]
  (let [raw-result (request/raw-article user-agent url)
        simple (extract/simple-prop-extract raw-result)
        lang (extract/languages-extract raw-result)
        thumb (extract/thumbnail-extract raw-result) 
        text  (extract/text-extract raw-result)]
    (apply merge [simple lang thumb text])))

(defn -main
  "json artcile from (media)wiki urls"
  [& args]
  (let [ [options args banner]
         (c/cli args
             ["-h" "--help" "print this help banner and exit" :flag true]
             ["-n" "--name" "the name that will be put in the request to the
                            media wiki API (typically yours)"]
             ["-u" "--user" "user-agent heder. Should include your mail"])]

    (when (options :help)
      (println banner)
      (System/exit 0))
    
    (when-not (options :user)
      (println "User agent is required! See --help for details")
      (System/exit 1))

    (let [user-agent (:user options)
          articles (map (partial article user-agent) args) ]
      (doseq [art articles]
        (p/pprint art)))))

