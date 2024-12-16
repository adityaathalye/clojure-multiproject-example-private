(ns system.sqlite
  "PER-DB PRAGMAS and settings include journal_mode,
   auto_vacuum. PER-CONNECTION PRAGMAS must be set fresh for each
   connection. To use CONNECTION POOLS, follow next.jdbc's advice. In all
   cases, map the underlying xerial driver's ENUM definition [1] to the
   actual SQLite PRAGMAs, because naming is hard and names
   change.

   Ref. PRAGMA names:
   https://github.com/xerial/sqlite-jdbc/blob/master/src/main/java/org/sqlite/SQLiteConfig.java#L382"
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [system.core :as system]
   [next.jdbc.connection :as jdbc-conn]
   [next.jdbc :as jdbc])
  (:import [com.zaxxer.hikari HikariDataSource])
  (:gen-class))

(defmethod system/build-config-map :system.sqlite
  [{{:keys [app-name runtime-environment-type]} :system.core/settings}]
  (log/info "Configuring module" (system/module-name))
  {;; Default pragmas
   [::defaults ::db-pragmas] {:journal_mode "WAL" ; supported by xerial JDBC driver
                              ;; INCREMENTAL = 2. Set manually. Not supported by xerial.
                              :auto_vacuum "INCREMENTAL"}
   [::defaults ::db-connection-pragmas] {:limit_worker_threads 1 ; assume limited CPU by default
                                         :busy_timeout 5000 ; ms, set per connection
                                         :foreign_keys "ON" ; ON = boolean 1, set per connection
                                         :cache_size -50000 ; KiB = 50 MiB, set per connection
                                         ;; NORMAL = 1, set per connection
                                         :synchronous "NORMAL"}
   [::defaults ::HikariCP] {:connectionTestQuery "PRAGMA journal_mode;"
                            :maximumPoolSize 1
                            :dataSourceProperties (ig/ref [::defaults ::db-connection-pragmas])}
   [::defaults ::C3P0] {:preferredTestQuery "PRAGMA journal_mode;"
                        :maxPoolSize 1
                        :dataSourceProperties (ig/ref [::defaults ::db-connection-pragmas])}
   ::primary {:dbtype "sqlite"
              :dbname (format "%s_%s_primary.sqlite3" app-name runtime-environment-type)
              :db-pragmas (ig/ref [::defaults ::db-pragmas])
              :cp-pragmas (ig/ref [::defaults ::HikariCP])}
   ::sessions {:dbtype "sqlite"
               :dbname (format "%s_%s_sessions.sqlite3" app-name runtime-environment-type)
               :db-pragmas (ig/ref [::defaults ::db-pragmas])
               :cp-pragmas (ig/ref [::defaults ::HikariCP])}})

(defmethod ig/init-key ::defaults
  [_ ctx]
  ctx)

;; (defmethod ig/init-key ::db
;;              [])
;; (prefer-method ig/init-key ::primary ::db)
;; (prefer-method ig/init-key ::sessions ::db)

(defmethod ig/init-key ::primary
  [_ {:keys [db-pragmas cp-pragmas]
      :as db-spec}]
  (let [db-spec (-> db-spec
                    (merge db-pragmas cp-pragmas)
                    (dissoc :db-pragmas :cp-pragmas))
        _ (log/info "==== PRIMARY db-spec ====" db-spec)
        reader-concurrency 4
        writer-concurrency 1
        reader ^HikariDataSource (jdbc-conn/->pool
                                  HikariDataSource
                                  (assoc-in db-spec
                                            [:dataSourceProperties :limit_worker_threads]
                                            reader-concurrency))
        writer ^HikariDataSource (jdbc-conn/->pool
                                  HikariDataSource
                                  (assoc-in db-spec
                                            [:dataSourceProperties :limit_worker_threads]
                                            writer-concurrency))
        ]
    (with-open [writer-conn (jdbc/get-connection writer)]
      ;; xerial's pragma ENUM properties do not include auto_vacuum,
      ;; so we have to do this manually, instead of via config. Sigh.
      (jdbc/execute! writer-conn [(format "PRAGMA auto_vacuum = %s"
                                          (:auto_vacuum db-spec))])
      ;; auto_vacuum value gets set only after vacuum
      (jdbc/execute! writer-conn ["VACUUM"])

      ;; Always run optimize at startup, after all
      ;; configurations are set and DDLs are executed.
      ;; https://www.sqlite.org/pragma.html#pragma_optimize
      (jdbc/execute! writer-conn ["PRAGMA optimize"]))

    (let [reader-conn (jdbc/get-connection reader)
          writer-conn (jdbc/get-connection writer)]
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
    {:reader reader
     :writer writer}))

(defmethod ig/init-key ::sessions
  [_ {:keys [db-pragmas cp-pragmas]
      :as db-spec}]
  (log/info "db-spec" db-spec)
  (let [db-spec (-> db-spec
                    (merge db-pragmas cp-pragmas)
                    (dissoc :db-pragmas :cp-pragmas))
        reader-concurrency 4
        writer-concurrency 1
        reader ^HikariDataSource (jdbc-conn/->pool HikariDataSource
                                                   (assoc db-spec :maximumPoolSize reader-concurrency))
        writer ^HikariDataSource (jdbc-conn/->pool HikariDataSource
                                                   (assoc db-spec :maximumPoolSize writer-concurrency))]
    {:reader reader
     :writer writer}))

(comment

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



  (ig/init (config "foobar" "dev")
           [::primary])

  (ig/init (config "foobar" "dev")
           [::sessions])

  (ig/init (config "foobar" "dev"))
  )
