(ns com.bombaylitmag.system.core
  (:require
   [clojure.tools.logging :as log]
   [com.bombaylitmag.handlers.core :as handlers-core]
   [integrant.core :as ig]
   [next.jdbc.connection :as jdbc-conn]
   [ring.adapter.jetty]
   [com.bombaylitmag.db.migrations :as db-migrate]
   [next.jdbc :as jdbc])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defn config
  [env]
  {:runtime/env env
   :database/primary {:dbtype "sqlite"
                      :dbname (format "db/mothra_%s.sqlite3" (name env))
                      :connectionTestQuery "VACUUM;"
                      ;; PRAGMAS
                      ;; - Map pragmas to the xerial driver's ENUM
                      ;;   https://github.com/xerial/sqlite-jdbc/blob/master/src/main/java/org/sqlite/SQLiteConfig.java#L382
                      ;; - Pass PRAGMA key/value pairs under :dataSourceProperties,
                      ;;   because we are using HikariCP for connection pooling.
                      ;;   Ref. next.jdbc's Connection Pooling advice
                      :dataSourceProperties {;; GLOBAL PRAGMAS
                                             :journal_mode "WAL"
                                             ;; PER CONNECTION PRAGMAS (must be set fresh for each connection)
                                             :temp_store 2 ; 2 = MEMORY, set per connection
                                             :busy_timeout 5000 ; ms, set per connection
                                             :limit_worker_threads 4 ; set per connection
                                             :foreign_keys 1 ; boolean 1 = ON, set per connection
                                             :cache_size -50000 ; KiB = 50 MiB, set per connection
                                             :synchronous 1 ; 1 = NORMAL, set per connection
                                             }}
   :database/sessions {:dbtype "sqlite"
                       :dbname (format "db/mothra_sessions_%s.sqlite3" (name env))}
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
  ;; No need to separate out read and write paths IF SQLite was
  ;; compiled to operate in `serialized` mode (which is the default)
  ;; ref: https://www.sqlite.org/threadsafe.html
  ;;
  ;; TODO: Test the read/write characteristics in WAL mode.

  (log/info "DB SETUP. PRIMARY STORE:" db-spec)
  (let [^HikariDataSource ds (jdbc-conn/->pool HikariDataSource
                                               db-spec)]
    ;; xerial's pragma ENUM does not define auto_vacuum,
    ;; so we have to do this manually. Sigh.
    (jdbc/execute! ds ["PRAGMA auto_vacuum = 2"]) ; 2 = INCREMENTAL
    (jdbc/execute! ds ["VACUUM"]) ; auto_vacuum value gets set only after vacuum
    ;; next.jdbc doc says to open/close a connection to
    ;; initialize the pool and perform the validation check:
    (.close (jdbc/get-connection ds))

    (with-open [c (jdbc/get-connection ds)]
      (doseq [pragma ["journal_mode"
                      "auto_vacuum"
                      "threads"
                      "temp_store"
                      "busy_timeout"
                      "foreign_keys"
                      "cache_size"
                      "synchronous"]]
        (log/info "DB SETUP. PRIMARY STORE PRAGMA IS:"
                  (jdbc/execute-one! c [(str "PRAGMA " pragma)]))))

    (db-migrate/migrate! ds)

    ;; Always run optimize at startup, after all
    ;; configurations are set and DDLs are executed.
    ;; https://www.sqlite.org/pragma.html#pragma_optimize
    (jdbc/execute! ds ["PRAGMA optimize"])
    ds))

(defmethod ig/halt-key! :database/primary
  [_ db-spec]
  (.close db-spec) ; HikariCP pool is closeable
  (log/info "Discarding DB. PRIMARY STORE:" db-spec)
  nil)

(defmethod ig/init-key :database/sessions
  [_ db-spec]
  (log/info "DB SETUP. SESSION STORE:" db-spec)
  (let [conn (jdbc-conn/->pool com.zaxxer.hikari.HikariDataSource
                               db-spec)]
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
  (user/reset)

   )
