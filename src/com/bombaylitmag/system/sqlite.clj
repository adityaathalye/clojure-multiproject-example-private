(ns com.bombaylitmag.system.sqlite
  (:import [com.zaxxer.hikari HikariDataSource]))

(def default-web-app-pragmas-per-db
  "Per-DB PRAGMAS are one-time database-level settings."
  {:journal_mode "WAL" ; supported by xerial JDBC driver
   ;; INCREMENTAL = 2. Set manually. Not supported by xerial.
   :auto_vacuum "INCREMENTAL"})

(def default-web-app-pragmas-per-connection
  "Per-connection PRAGMAs are common to reader and writer paths,
  presumably configured using a connection pool."
  {:limit_worker_threads 1 ; assume limited CPU by default
   :busy_timeout 5000 ; ms, set per connection
   :foreign_keys "ON" ; ON = boolean 1, set per connection
   :cache_size -50000 ; KiB = 50 MiB, set per connection
   ;; NORMAL = 1, set per connection
   :synchronous "NORMAL"})

(def default-connection-pool-configs
  {:HikariCP
   {:connectionTestQuery "PRAGMA journal_mode;"
    :maximumPoolSize 1
    :dataSourceProperties default-web-app-pragmas-per-connection}
   :C3P0
   {:preferredTestQuery "PRAGMA journal_mode;"
    :maxPoolSize 1
    :dataSourceProperties default-web-app-pragmas-per-connection}})

(defn make-db-spec
  "GLOBAL PRAGMAS and settings include journal_mode, auto_vacuum

   PER-CONNECTION PRAGMAS for use via HikariCP
   - Pass PRAGMA key/value pairs under :dataSourceProperties,
     as per next.jdbc's advice for Connection Pooling with HikariCP.

   - Map the underlying xerial driver's ENUM definition to the actual
     SQLite PRAGMAs, because naming is hard and names change.
     https://github.com/xerial/sqlite-jdbc/blob/master/src/main/java/org/sqlite/SQLiteConfig.java#L382

   - These properties must be set fresh for each connection."
  ([dbname]
   (make-db-spec dbname
                 default-web-app-pragmas-per-db
                 :HikariCP))
  ([dbname db-pragmas]
   (make-db-spec dbname
                 db-pragmas
                 :HikariCP))
  ([dbname db-pragmas connection-pool-name]
   (merge {:dbtype "sqlite", :dbname dbname}
          db-pragmas
          (get default-connection-pool-configs
               connection-pool-name))))
