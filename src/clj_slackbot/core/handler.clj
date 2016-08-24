(ns clj-slackbot.core.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [clojail.core :refer [sandbox]]
            [clojail.testers :refer [secure-tester-without-def blacklist-objects blacklist-packages blacklist-symbols blacklist-nses blanket]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [clj-http.client :as client]
            [clojure.algo.monads]
            [clojure.data.json]
            [clojure.math.numeric-tower]
            [clojure.test.check]
            [cemerick.pomegranate])
  (:import java.io.StringWriter
           java.util.concurrent.TimeoutException)
  (:gen-class))

(def insecure-tester
  [(blacklist-objects [])
                      ;  clojure.lang.Compiler clojure.lang.Ref clojure.lang.Reflector
                      ;  clojure.lang.Namespace clojure.lang.Var clojure.lang.RT
                      ;  java.io.ObjectInputStream
   (blacklist-packages [])
                        ; "java.lang.reflect"
                        ; "java.security"
                        ; "java.util.concurrent"
                        ; "java.awt"
   (blacklist-symbols
    '#{})
      ;  alter-var-root intern eval catch
      ;  load-string load-reader addMethod ns-resolve resolve find-var
      ;  *read-eval* ns-publics ns-unmap set! ns-map ns-interns the-ns
      ;  push-thread-bindings pop-thread-bindings future-call agent send
      ;  send-off pmap pcalls pvals in-ns System/out System/in System/err
      ;  with-redefs-fn Class/forName
   (blacklist-nses '[clojure.main])
   (blanket "clojail")])

(def clj-slackbot-tester
  (conj insecure-tester (blanket "clj-slackbot" "compojure" "ring")))

(def sb (sandbox clj-slackbot-tester))

(def post-url
  (:post-url env))

(def command-token
  (:command-token env))

(defn post-to-slack
  ([s channel]
   (let [p (if channel {:channel channel} {})]
     (client/post post-url
                  {:content-type :json
                   :form-params (assoc p :text s)})))
  ([s]
   (post-to-slack s nil)))

(defn eval-expr
  "Evaluate the given string"
  [s]
  (try
    (with-open [out (StringWriter.)]
      (let [form (binding [*read-eval* false] (read-string s))
            result (sb form {#'*out* out})]
        {:status true
         :input s
         :form form
         :result result
         :output (.toString out)}))
    (catch Exception e
      {:status false
       :input s
       :result (.getMessage e)})))

(defn format-result [r user]
  (if (:status r)
    (str "```"
         ";; " user "\n"
         (:input r) "\n"
         "=> " (:form r) "\n"
         (when-let [o (:output r)]
           o)
         (if (nil? (:result r))
           "nil"
           (prn-str (:result r)))
         "```")
    (str "```"
         ";; " user "\n"
         "==> " (or (:form r) (:input r)) "\n"
         (or (:result r) "Unknown Error")
         "```")))

(defn eval-and-post [s user channel]
  (-> s
      eval-expr
      (format-result user)
      (post-to-slack channel)))

(defn handle-clj [params]
  (if-not (= (:token params) command-token)
    {:status 403 :body "Unauthorized"}
    (let [channel (condp = (:channel_name params)
                    "directmessage" (str "@" (:user_name params))
                    "privategroup" (:channel_id params)
                    (str "#" (:channel_name params)))]
      (eval-and-post (:text params) (:user_name params) channel)
      {:status 200
       :body ""
       :headers {"Content-Type" "text/plain"}})))

(defroutes approutes
  (POST "/clj" req (handle-clj (:params req)))
  (route/not-found "Not Found"))

(def app (wrap-defaults approutes
                        api-defaults))

(defn -main [& args]
  (run-jetty app {:port (Integer/parseInt (or (:port env)
                                              "3000"))}))
