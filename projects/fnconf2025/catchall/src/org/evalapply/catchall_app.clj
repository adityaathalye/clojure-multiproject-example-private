(ns org.evalapply.catchall-app
  (:require [ring.adapter.jetty :as jetty])
  (:gen-class))

(defn echo-handler [request]
  {:status 200
   :headers {"Content-Type" "text/html;charset=utf-8"}
   :body (pr-str request)})

(defn run-jetty
  [port]
  (println "Starting Jetty server at port:" port)
  (jetty/run-jetty echo-handler
                   {:port port :join? false}))

(defn -main []
  (run-jetty 3000))

(comment ; "'Rich' comment form"
  ;; clojure -M -m org.evalapply.catchall-app
  ;; OR:
  (def server (run-jetty 3001))

  ;; Extra: REPL-driven-development techniques.

  ;; Inspect live object
  (do (require 'clojure.reflect)
      (clojure.reflect/reflect server))

  ;; Capture values to inspect them at will.
  (def responses (atom []))

  (defn capture-response
    [response]
    (swap! responses conj response))

  (add-tap capture-response)

  (tap> {:status 200 :body "hi"})

  ;; Try Out dependencies
  ;; - Add without modifying deps.edn file
  ;; - refactor echo-handler to handle json=true query parameter
  ;;   - copy down and replace body (dynamic REPL-driven dev.)
  (require 'clojure.repl.deps)

  (clojure.repl.deps/add-libs
   {'org.clojure/data.json {:mvn/version "2.5.1"}
    'clj-http/clj-http {:mvn/version "3.12.3"}})

  (require '[clojure.data.json :as json])
  (defn TODO-override-echo-handler [])

  (require '[clj-http.client :as http])
  (let [base-uri "http://localhost:3001"]
    (http/get (str base-uri "/foobar?json=true"))))
