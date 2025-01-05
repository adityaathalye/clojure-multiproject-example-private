(ns com.adityaathalye.grugstack.system.runtime
  (:require
    [integrant.core :as ig]
    [clojure.tools.logging :as log]
    [com.adityaathalye.grugstack.system.core :as system])
  (:gen-class))

(defmethod system/build-config-map :system.runtime
  [{{:keys [app-name runtime-environment-type]} :com.adityaathalye.grugstack.system.core/settings}]
  (log/info "Configuring module" (system/module-name))
  {::environment {:type runtime-environment-type
                  :app-name app-name
                  :total-memory (.totalMemory (Runtime/getRuntime))
                  :available-processors (.availableProcessors (Runtime/getRuntime))
                  :version (.toString (Runtime/version))}})

(defmethod ig/init-key ::environment
  [_ runtime-map]
  (log/info "Runtime environment is:" runtime-map)
  runtime-map)
