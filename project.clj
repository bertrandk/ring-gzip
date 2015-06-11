(defproject bk/ring-gzip "0.1.1"
  :description "Middleware that compresses repsonses with gzip for supported
               user-agents."
  :url "https://github.com/bertrandk/ring-gzip"
  :license {:name "MIT-style license (see LICENSE for details)."}
  :global-vars {*warn-on-reflection* true}
  :profiles {:test {:resource-paths ["test"]}}
  :dependencies [[org.clojure/clojure "1.5.1"]])
