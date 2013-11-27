(defproject wikison "0.1.1"
  :description "json documents from wiki pages"
  :url "http://github.con/jopela/wikison"
  :license {:name "GNU GENERAL PUBLIC LICENSE"
            :url "http://www.gnu.org/licenses/gpl-3.0.txt"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.7.7"]
                 [org.clojure/tools.cli "0.2.4"]
                 [org.clojure/data.json "0.2.3"]
                 [instaparse "1.2.12"]
                 [org.clojure/core.match "0.2.0"]
                 [hiccup "1.0.4"]]
  :main ^:skip-aot wikison.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :scm {:name "git"
        :url "https://github.com/jopela/wikison"})
