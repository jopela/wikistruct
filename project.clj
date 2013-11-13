(defproject wikison "0.1.0-alpha"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "GNU GENERAL PUBLIC LICENSE"
            :url "http://www.gnu.org/licenses/gpl-3.0.txt"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.7.7"]
                 [org.clojure/tools.cli "0.2.4"]]
  :main ^:skip-aot wikison.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
