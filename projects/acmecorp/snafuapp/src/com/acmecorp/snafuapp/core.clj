(ns com.acmecorp.snafuapp.core
  (:require [com.adityaathalye.grugstack.system.core :as system]
            [clojure.tools.logging :as log]
            [com.adityaathalye.grugstack.settings.core :as settings]
            [com.acmecorp.snafuapp.handlers.core])
  (:gen-class))

(defn -main
  [& args]
  (let [env (or (first args) :dev)
        env (keyword env)
        settings (settings/make-settings
                  (settings/read-settings! "com/acmecorp/snafuapp/settings.edn")
                  {:system.server/reitit-route-tree com.acmecorp.snafuapp.handlers.core/reitit-route-tree})]
    (log/info "Invoking -main with environment" env)
    (system/init settings)))
