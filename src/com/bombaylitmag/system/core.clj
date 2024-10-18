(ns com.bombaylitmag.system.core
  (:require
   [clojure.tools.logging :as log]
   [com.bombaylitmag.handlers.core :as handlers-core]
   [integrant.core :as ig]
   [next.jdbc :as jdbc]
   [next.jdbc.connection :as jdbc-conn]
   [ring.adapter.jetty])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defn config
  [env]
  {:runtime/env env
   :database/primary {:dbtype "sqlite"
                      :dbname (format "mothra_%s.sqlite3" (name env))}
   :database/sessions {:dbtype "sqlite"
                       :dbname (format "mothra_sessions_%s.sqlite3" (name env))}
   :handler/run-app {:system {:db (ig/ref :database/primary)
                              :sessions (ig/ref :database/sessions)
                              :env (ig/ref :runtime/env)}}
   :adapter/jetty {:handler (ig/ref :handler/run-app) :port 3000}})

(defmethod ig/init-key :runtime/env
  [_ env]
  (log/info "Runtime environment is:" env)
  env)

(defmethod ig/init-key :database/primary
  [_ db-spec]
  (log/info "Connecting DB. PRIMARY STORE:" db-spec)
  ;; No need to separate out read and write paths IF SQLite was
  ;; compiled to operate in `serialized` mode (which is the default)
  ;; ref: https://www.sqlite.org/threadsafe.html
  ;;
  ;; TODO: Test the read/write characteristics in WAL mode.
  (let [conn (jdbc-conn/->pool com.zaxxer.hikari.HikariDataSource
                               db-spec)]
    (log/info "Migrating DB. PRIMARY STORE:" db-spec)
    #_(populate conn (:dbtype db-spec))
    conn))

(defmethod ig/halt-key! :database/primary
  [_ db-spec]
  (log/info "Discarding DB. PRIMARY STORE:" db-spec)
  nil)

(defmethod ig/init-key :database/sessions
  [_ db-spec]
  (log/info "Connecting DB. SESSION STORE:" db-spec)
  (let [conn (jdbc-conn/->pool com.zaxxer.hikari.HikariDataSource
                               db-spec)]
    (log/info "Migrating DB. SESSION STORE:" db-spec)
    #_(populate conn (:dbtype db-spec))
    conn))

(defmethod ig/halt-key! :database/sessions
  [_ db-spec]
  (log/info "Discarding DB. SESSION STORE:" db-spec)
  nil)

(defmethod ig/init-key :handler/run-app
  [_ {:keys [system]}]
  (handlers-core/app system))

(defmethod ig/init-key :adapter/jetty
  [_ {:keys [handler] :as opts}]
  (log/info "Starting Jetty server at port: " (:port opts))
  (ring.adapter.jetty/run-jetty handler
                                (-> opts
                                    (dissoc handler)
                                    (assoc :join? false))))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (log/info "Stopping Jetty server.")
  (.stop server))

(defn init
  [env]
  (ig/init (config env)))

(comment

   )
