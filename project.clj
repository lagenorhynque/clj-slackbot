(defproject clj-slackbot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[cheshire "5.8.1"]
                 [clj-http "3.10.0"]
                 [clojail "1.0.6"]
                 [com.cemerick/pomegranate "1.1.0"]
                 [compojure "1.6.1"]
                 [environ "1.0.2"]
                 [funcool/beicon "5.0.0"]
                 [funcool/cats "2.3.2"]
                 [funcool/promesa "2.0.1"]
                 [manifold "0.1.8"]
                 [org.clojure/algo.monads "0.1.6"]
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/core.match "0.3.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojure/test.check "0.9.0"]
                 [prismatic/schema "1.1.10"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-jetty-adapter "1.7.1"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler clj-slackbot.core.handler/app}
  :uberjar-name "clj-slackbot.jar"
  :main clj-slackbot.core.handler
  :profiles
  {:dev {:repl-options {:init-ns clj-slackbot.core.handler}
         :dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}
   :uberjar {:aot :all}})
