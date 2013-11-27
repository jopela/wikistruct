(ns wikison.extract
  "Code that extract several properties from the a raw article"
  (:require [clojure.tools.cli :as c]
            [clojure.set :as cset]
            [wikison.parse :as parse]
            [wikison.filters :as filters-func]
            ))

(defn simple-prop-extract
  "Extract the simple properties (ones that directly map to a property in the
  result) from the  raw-article
  result" 
  [raw]
  (let [new-raw (cset/rename-keys raw
                                  {:fullurl :url
                                   :pagelanguage :lang})]
    (select-keys new-raw [:url :title :pageid :lang])))

(defn languages-extract
  "extract the languages from the  raw-article
  result"
  [raw]
  (let [raw-languages (raw :langlinks)]
    {:other-langs (vec (map :lang raw-languages))}))

(defn thumbnail-extract
  "extract the thumbnail from the  raw-article
  result"
  [raw]
  {:depiction (-> raw :thumbnail :source)})

(defn text-extract
  "extract the desired form of article text from the raw wiki-creole"
  [filters eval-func raw]
  (let [parse-tree (parse/wiki-creole-parse raw)
        pipeline (apply comp filters)
        filtered-parse-tree (pipeline parse-tree)]
    (eval-func filtered-parse-tree)))
