(ns system.core
  (:require
   [clojure.tools.logging :as log]
   [com.bombaylitmag.handlers.core :as handlers-core]
   [integrant.core :as ig]
   [next.jdbc.connection :as jdbc-conn]
   [ring.adapter.jetty]
   [com.bombaylitmag.db.migrations :as db-migrate]
   [com.bombaylitmag.db.utils :as db-utils]
   [next.jdbc :as jdbc])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defn config
  [env]
  {:system.runtime/environment {:type env
                                :total-memory (.totalMemory (Runtime/getRuntime))
                                :available-processors (.availableProcessors (Runtime/getRuntime))
                                :version (.toString (Runtime/version))}
   :system.database/primary (assoc (ig/ref :settings/sqlite)
                                   :dbname (format "db/mothra_%s.sqlite3" (name env)))
   :system.database/sessions (assoc (ig/ref :database/sqlite)
                                    :dbname (format "db/mothra_sessions_%s.sqlite3" (name env)))
   :system.app/context {:db (ig/ref :database/primary)
                        :sessions (ig/ref :database/sessions)
                        :env (:env (ig/ref :runtime/runtime))}
   :system.app/handler (ig/ref :system.app/context)
   :system.app/server {:handler (ig/ref :system.app/handler) :port 3000}})

(defn init
  [env]
  (ig/init (config env)))

(defmethod ig/init-key :runtime/env
  [_ env]
  (log/info "Runtime environment is:" env)
  env)

(defmethod ig/init-key :runtime/runtime
  [_ config]
  config)

(defmethod ig/init-key :database/primary
  [_ db-spec]
  ;; No need to separate out read and write paths IF SQLite was
  ;; compiled to operate in `serialized` mode (which is the default)
  ;; ref: https://www.sqlite.org/threadsafe.html
  ;;
  ;; TODO: Test the read/write characteristics in WAL mode.
  (log/info "DB SETUP. PRIMARY STORE:" (select-keys db-spec-common [:dbtype :dbname]))
  (let [reader-pool ^HikariDataSource (->> db-spec-read-only
                                           (merge-with merge db-spec-common)
                                           (jdbc-conn/->pool HikariDataSource))
        writer-pool ^HikariDataSource (->> db-spec-read-write
                                           (merge-with merge db-spec-common)
                                           (jdbc-conn/->pool HikariDataSource))]
    ;; next.jdbc doc says to fetch and then open/close a connection
    ;; to initialize a pool and perform validation check.
    ;; Using with-open on the read and write paths does it for us.
    (with-open [writer-conn (jdbc/get-connection writer-pool)]
      ;; xerial's pragma ENUM properties do not include auto_vacuum,
      ;; so we have to do this manually, instead of via config. Sigh.
      (jdbc/execute! writer-conn ["PRAGMA auto_vacuum = ?"
                                  (:auto-vacuum db-spec-common)])
      ;; auto_vacuum value gets set only after vacuum
      (jdbc/execute! writer-conn ["VACUUM"])

      ;; Always run optimize at startup, after all
      ;; configurations are set and DDLs are executed.
      ;; https://www.sqlite.org/pragma.html#pragma_optimize
      (jdbc/execute! writer-conn ["PRAGMA optimize"]))

    (with-open [writer-conn (jdbc/get-connection writer-pool)
                reader-conn (jdbc/get-connection reader-pool)]
      (doseq [pragma ["journal_mode"
                      "auto_vacuum"
                      "threads"
                      "temp_store"
                      "busy_timeout"
                      "foreign_keys"
                      "cache_size"
                      "synchronous"]]
        (log/info "DB SETUP. PRIMARY STORE."
                  "READER POOL PRAGMA:"
                  (jdbc/execute-one! reader-conn [(str "PRAGMA " pragma)])
                  "WRITER POOL PRAGMA:"
                  (jdbc/execute-one! writer-conn [(str "PRAGMA " pragma)]))))

    ;; Construct DB connector with separate write path and read path
    ;; suited for SQLite in WAL mode.
    (reify
      db-utils/IDatabaseConnectionPool
      (reader [_] (jdbc/get-connection reader-pool))
      (writer [_] (jdbc/get-connection writer-pool)))))

(defmethod ig/halt-key! :database/primary
  [_ db-spec]
  ;; HikariCP pool is closeable
  (.close (db-utils/reader db-spec))
  (.close (db-utils/writer db-spec))
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
  [_ {:keys [db sessions env] :as system}]
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
                                    (assoc :join? false))))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (log/info "Stopping Jetty server.")
  (.stop server))

(comment
  (user/reset)

  (require '[integrant.repl.state :as ig-state])

  (let [ds (get-in ig-state/system [:database/primary :reified])]
    [(db-utils/r ds)
     (db-utils/w ds)])

  (clojure.reflect/reflect (Runtime/getRuntime)))
