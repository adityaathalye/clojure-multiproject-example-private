(ns com.adityaathalye.grugstack.system.db.sqlite
  "PER-DB PRAGMAS and settings include journal_mode,
   auto_vacuum. PER-CONNECTION PRAGMAS must be set fresh for each
   connection. To use CONNECTION POOLS, follow next.jdbc's advice. In all
   cases, map the underlying xerial driver's ENUM definition to the
   actual SQLite PRAGMAs, because naming is hard and names
   change. Ref. PRAGMA names:
   https://github.com/xerial/sqlite-jdbc/blob/master/src/main/java/org/sqlite/SQLiteConfig.java#L382

  Connection Pool choice is singular. C3P0 is an alternative to HikariCP."
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [com.adityaathalye.grugstack.system.core :as system]
   [next.jdbc.connection :as jdbc-conn]
   [next.jdbc :as jdbc])
  (:import [com.zaxxer.hikari HikariDataSource])
  (:gen-class))

(defmethod system/build-config-map :com.adityaathalye.grugstack.system.db.sqlite
  [{{:keys [dataSourceProperties]
     :or {dataSourceProperties {:limit_worker_threads 4
                                :busy_timeout 5000 ; ms, set per connection
                                :foreign_keys "ON" ; ON = boolean 1, set per connection
                                :cache_size -50000 ; KiB = 50 MiB, set per connection
                                ;; NORMAL = 1, set per connection
                                :synchronous "NORMAL"}}}
    :com.adityaathalye.grugstack.system.db.sqlite/db-spec
    :as ctx}]
  {;; Default pragmas
   ::db-spec {:dbtype "sqlite"
              :journal_mode "WAL" ; supported by xerial JDBC driver
              ;; INCREMENTAL = 2. Set manually. Not supported by xerial.
              :auto_vacuum "INCREMENTAL"
              :connectionTestQuery "PRAGMA journal_mode;" ; used by HikariCP
              :preferredTestQuery "PRAGMA journal_mode;" ; used by C3P0
              ;; :maximumPoolSize max-concurrency ; not supported by Xerial
              :dataSourceProperties dataSourceProperties}})

(defmethod ig/init-key ::db-spec
  [_ ctx]
  ctx)

(defn get-pragma-settings
  ([connection-pool]
   (get-pragma-settings connection-pool
                        ["journal_mode"
                         "auto_vacuum"
                         "threads"
                         "temp_store"
                         "busy_timeout"
                         "foreign_keys"
                         "cache_size"
                         "synchronous"]))
  ([connection-pool pragmas]
   (with-open [connection (jdbc/get-connection connection-pool)]
     (reduce (fn [db-settings pragma]
               (into db-settings
                     (jdbc/execute-one! connection [(str "PRAGMA " pragma)])))
             {}
             pragmas))))

(defn set-up!
  [dbname {:keys [dbtype] :as db-spec}]
  (let [db-spec (assoc db-spec :dbname dbname)
        datasource ^HikariDataSource (jdbc-conn/->pool
                                      HikariDataSource
                                      db-spec)]
    (log/info "==== Starting DB with db-spec ====" db-spec)
    ;; next.jdbc doc says to fetch and then open/close a connection
    ;; to initialize a pool and perform validation check.
    ;; Using with-open on the read and write paths does it for us.
    (with-open [connection (jdbc/get-connection datasource)]
      ;; xerial's pragma ENUM properties do not include auto_vacuum,
      ;; so we have to do this manually, instead of via config. Sigh.
      (jdbc/execute! connection [(format "PRAGMA auto_vacuum = %s"
                                         (:auto_vacuum db-spec))])
      ;; auto_vacuum value gets set only after vacuum
      (jdbc/execute! connection ["VACUUM"])

      ;; Always run optimize at startup, after all
      ;; configurations are set and DDLs are executed.
      ;; https://www.sqlite.org/pragma.html#pragma_optimize
      (jdbc/execute! connection ["PRAGMA optimize"]))
    (log/info "Initialized pooled datasource PRAGMAS:" (get-pragma-settings datasource))
    datasource))
