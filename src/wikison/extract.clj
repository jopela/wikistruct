(ns wikison.extract
  "Code that extract several properties from the a raw article"
  (:require [clojure.tools.cli :as c]
            [clojure.string :as string]
            [clojure.set :as cset]
            [clojure.data.json :as json]
            [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.pprint :as p]))

;cat extract
(defn simple-prop-extract
  "Extract the simple properties (ones that directly map to a property in the
  json result) from the  raw-article
  result" 
  [raw]
  (let [new-raw (cset/rename-keys raw
                                  {:fullurl :url
                                   :pagelanguage :lang})]
    (select-keys new-raw [:url :title :pageid :lang])))

;cat extract
(defn languages-extract
  "extract the languages from the  raw-article
  result"
  [raw]
  (let [raw-languages (raw :langlinks)]
    {:other-langs (vec (map :lang raw-languages))}))

;cat extract
(defn thumbnail-extract
  "extract the thumbnail from the  raw-article
  result"
  [raw]
  {:depiction (-> raw :thumbnail :source)})

