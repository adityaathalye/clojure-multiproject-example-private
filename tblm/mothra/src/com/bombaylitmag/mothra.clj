(ns com.bombaylitmag.mothra
  (:require [system.core :as system]
            [clojure.tools.logging :as log]
            [settings.core :as settings]
            [com.bombaylitmag.handlers.core])
  (:gen-class))

(defn -main
  [& args]
  (let [env (or (first args) :dev)
        env (keyword env)
        settings (settings/make-settings
                  (settings/read-settings! "com/bombaylitmag/settings.edn")
                  {:system.server/reitit-route-tree com.bombaylitmag.handlers.core/reitit-route-tree})]
    (log/info "Invoking -main with environment" env)
    (system/init settings)))
