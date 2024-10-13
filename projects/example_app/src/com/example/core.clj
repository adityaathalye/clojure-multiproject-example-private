(ns com.example.core
  (:gen-class))

(defn greet
  []
  (println "Hello, world."))

(defn -main
  [& _args]
  (greet))
