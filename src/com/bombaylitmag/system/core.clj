(ns com.bombaylitmag.system.core
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
  {:runtime/runtime {:env env
                     :total-memory (.totalMemory (Runtime/getRuntime))
                     :available-processors (.availableProcessors (Runtime/getRuntime))
                     :version (.toString (Runtime/version))}
   :database/primary {:db-spec-common
                      {;; GLOBAL PRAGMAS and settings
                       :dbtype "sqlite"
                       :dbname (format "db/mothra_%s.sqlite3" (name env))
                       :journal_mode "WAL" ; supported by xerial JDBC driver
                       :auto_vacuum 2 ; not supported by xerial. Set manually.
                       :connectionTestQuery "VACUUM;"
                       ;; PER-CONNECTION PRAGMAS for use via HikariCP
                       ;; - Pass PRAGMA key/value pairs under :dataSourceProperties,
                       ;;   as per next.jdbc's advice for Connection Pooling with HikariCP.
                       ;; - Map the underlying xerial driver's ENUM definition to the actual
                       ;;   SQLite PRAGMAs, because naming is hard and names change.
                       ;;   https://github.com/xerial/sqlite-jdbc/blob/master/src/main/java/org/sqlite/SQLiteConfig.java#L382
                       ;; - These properties must be set fresh for each connection.
                       :dataSourceProperties {;; Properties common to reader and writer
                                              :temp_store 2 ; 2 = MEMORY, set per connection
                                              :busy_timeout 5000 ; ms, set per connection
                                              :foreign_keys 1 ; boolean 1 = ON, set per connection
                                              :cache_size -50000 ; KiB = 50 MiB, set per connection
                                              :synchronous 1 ; 1 = NORMAL, set per connection
                                              }}
                      :db-spec-read-only {::dataSourceProperties
                                          {;; make writes fail
                                           :read-only true
                                           ;; use all available processors
                                           :limit_worker_threads (:available-processors (ig/ref :runtime/runtime))}}
                      :db-spec-read-write {::dataSourceProperties
                                           {;; allow writes and reads
                                            :read-only false
                                            ;; permit only one writer at a time
                                            :limit_worker_threads 1}}}

   :database/sessions {:dbtype "sqlite"
                       :dbname (format "db/mothra_sessions_%s.sqlite3" (name env))}
   :handler/run-app {:db (ig/ref :database/primary)
                     :sessions (ig/ref :database/sessions)
                     :env (:env (ig/ref :runtime/runtime))}
   :adapter/jetty {:handler (ig/ref :handler/run-app) :port 3000}})

(defmethod ig/init-key :runtime/env
  [_ env]
  (log/info "Runtime environment is:" env)
  env)

(defmethod ig/init-key :runtime/runtime
  [_ config]
  config)

(defmethod ig/init-key :database/primary
  [_ {:keys [db-spec-common
             db-spec-read-only
             db-spec-read-write]}]
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

(defn init
  [env]
  (ig/init (config env)))

(comment
  (user/reset)

  (require '[integrant.repl.state :as ig-state])

  (let [ds (get-in ig-state/system [:database/primary :reified])]
    [(db-utils/r ds)
     (db-utils/w ds)])

  (clojure.reflect/reflect (Runtime/getRuntime)))
