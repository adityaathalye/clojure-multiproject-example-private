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
   [system.core :as system])
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
   [::db ::primary] {:dbtype "sqlite"
                     :dbname (format "%s_%s_primary.sqlite3" app-name runtime-environment-type)
                     :db-pragmas (ig/ref [::defaults ::db-pragmas])
                     :cp-pragmas (ig/ref [::defaults ::HikariCP])}
   [::db ::sessions] {:dbtype "sqlite"
                      :dbname (format "%s_%s_sessions.sqlite3" app-name runtime-environment-type)
                      :db-pragmas (ig/ref [::defaults ::db-pragmas])
                      :cp-pragmas (ig/ref [::defaults ::HikariCP])}})

(defmethod ig/init-key ::defaults
  [_ ctx]
  ctx)

(defmethod ig/init-key ::db
  [_ {:keys [db-pragmas cp-pragmas]
      :as db-spec}]
  (log/info "db-spec" db-spec)
  (-> db-spec
      (merge db-spec db-pragmas cp-pragmas)
      (dissoc :db-pragmas :cp-pragmas)))

(comment
  (ig/init (config "foobar" "dev")
           [::primary])

  (ig/init (config "foobar" "dev")
           [::sessions])

  (ig/init (config "foobar" "dev"))
  )
