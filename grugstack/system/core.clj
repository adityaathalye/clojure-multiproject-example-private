(ns system.core
  "OR pull in a library of pre-built Component components under the
  'system' directory. ORrrrr vendor the code for visibility. e.g. If we
  add danielsz/system as a git submodule (the only, and the only sane
  way to use submodules) under our 'system' directory, then BOOM, we
  have all the recipes. https://danielsz.github.io/system/"
  (:require
   [clojure.tools.logging :as log]
   ;; [com.bombaylitmag.handlers.core :as handlers-core]
   [integrant.core :as ig]
   [next.jdbc.connection :as jdbc-conn]
   [ring.adapter.jetty]
   ;; [com.bombaylitmag.db.migrations :as db-migrate]
   ;; [com.bombaylitmag.db.utils :as db-utils]
   [next.jdbc :as jdbc]
   [settings.core :as settings])
  (:gen-class))

(defn module-name
  []
  (keyword (ns-name *ns*)))

#_(def build-config-map nil)
(defmulti build-config-map
  ::module)

(defmethod ig/init-key ::settings
  [_ {:keys [app-name runtime-environment-type]
      :as settings}]
  (assoc settings
         :app-name (name app-name)
         :runtime-environment (name runtime-environment-type)))

(defn expand
  "The way init works, the ::settings map never appears in the
   config. This is intentional. Settings can contain secrets that we
   don't want to have to remember to elide from the final configuration."
  [{{:keys [system-modules]} :system.core/settings
    :as settings}]
  (apply require (map symbol system-modules))
  (let [cfg (reduce (fn [system-configuration module-name]
                      (merge system-configuration
                             (build-config-map (assoc settings
                                                      ::module module-name))))
                    {}
                    system-modules)]
    (-> cfg
        (dissoc :system.core/settings)
        ig/expand)))

(defn init
  [settings]
  (let [cfg (expand settings)]
    (ig/load-namespaces cfg)
    (-> cfg ig/init)))

(comment
  (let [settings (settings/make-settings
                  (settings/read-settings! "com/example/settings.edn"))]
    (init settings))
  )

(comment
  (defmethod ig/init-key ::handler
    [_ handler-context]
    (log/info (format "Running app handler in %s environment method with config %s"
                      env system))
    (with-open [cr (jdbc/get-connection (db-utils/reader db))
                cw (jdbc/get-connection (db-utils/writer db))])
    (handlers-core/app system))

  (defmethod ig/init-key :adapter/jetty
    [_ {:keys [handler] :as opts}]
    (log/info "Starting Jetty server at port: " (:port opts))
    (ring.adapter.jetty/run-jetty handler
                                  (-> opts
                                      (dissoc handler)
                                      (assoc :join? false)))))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (log/info "Stopping Jetty server.")
  (.stop server))

(comment
  (settings/make-settings {:app-name "testing-system"})

  (user/reset)

  (clojure.reflect/reflect (Runtime/getRuntime)))
