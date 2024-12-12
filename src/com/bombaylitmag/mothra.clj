(ns com.bombaylitmag.mothra
  (:require [system.core :as system]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn -main
  [& args]
  (let [env (or (first args) :dev)
        env (keyword env)]
    (log/info "Invoking -main with environment" env)
    (system/init "mothra" env)))
