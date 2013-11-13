(ns wikison.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.tools.cli :as c]))

(defn -main
  "json artcile from (media)wiki urls"
  [& args]
  (let [ [options args banner]
         (c/cli args
             ["-h" "--help" "print this help banner and exit" :flag true]) ]

    (when (options :help)
      (println banner)
      (System/exit 0))
    
    (println "No options given")))


(defn article 
  "return a json document built from the given url"
  [url]
  {})

(defn mediawiki-req
  "Fowards a request to the (media)wiki api"
  [url params]
  (let [req-url (api-url url)
        ; TODO: put the very frequently used, rarely overridden params here
        req-params (merge {"format" "json" "action" "query"} params)
        req-format (-> req-params "format" keyword ) ]
    (client/get req-url {:query-params req-params :as req-format})))
    
  






