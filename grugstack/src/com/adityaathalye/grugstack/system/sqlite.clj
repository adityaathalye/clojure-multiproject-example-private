(ns com.adityaathalye.grugstack.system.sqlite
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

(defmethod system/build-config-map :com.adityaathalye.grugstack.system.sqlite
  [{{:keys [app-name runtime-environment-type]} :com.adityaathalye.grugstack.system.core/settings}]
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
   [::defaults ::connection-pool] {:connectionTestQuery "PRAGMA journal_mode;"
                                   :maximumPoolSize 4 ; use only for Read path concurrency
                                   :dataSourceProperties (ig/ref [::defaults ::db-connection-pragmas])}
   ;; If one wants C3PO instead of HikariCP
   ;; [::defaults ::connection-pool] {:preferredTestQuery "PRAGMA journal_mode;"
   ;;                                 :maxPoolSize 4
   ;;                                 :dataSourceProperties (ig/ref [::defaults ::db-connection-pragmas])}
   [::db ::primary] {:dbtype "sqlite"
                     :dbname (format "%s_%s_primary.sqlite3" app-name runtime-environment-type)
                     :db-pragmas (ig/ref [::defaults ::db-pragmas])
                     :cp-pragmas (ig/ref [::defaults ::connection-pool])}
   [::db ::sessions] {:dbtype "sqlite"
                      :dbname (format "%s_%s_sessions.sqlite3" app-name runtime-environment-type)
                      :db-pragmas (ig/ref [::defaults ::db-pragmas])
                      :cp-pragmas (ig/ref [::defaults ::connection-pool])}})

(defmethod ig/init-key ::defaults
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

(defn set-up-sqlite!
  [{:keys [dbname dbtype db-pragmas cp-pragmas]
    :as db-spec}]
  (let [db-spec (-> db-spec
                    (merge db-pragmas cp-pragmas)
                    (dissoc :db-pragmas :cp-pragmas))
        max-concurrency (max (:maximumPoolSize cp-pragmas)
                             1)
        datasource ^HikariDataSource (jdbc-conn/->pool
                                      HikariDataSource
                                      (assoc-in db-spec
                                                [:dataSourceProperties :limit_worker_threads]
                                                max-concurrency))]
    (log/info "==== Starting DB with db-spec ====" db-spec)
    ;; next.jdbc doc says to fetch and then open/close a connection
    ;; to initialize a pool and perform validation check.
    ;; Using with-open on the read and write paths does it for us.
    (with-open [writer-conn (jdbc/get-connection datasource)]
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
    (log/info "Initialized pooled datasource PRAGMAS:" (get-pragma-settings datasource))
    {:dbname dbname
     :dbtype dbtype
     :ds datasource}))

(defmethod ig/init-key ::db
  [_ db-spec]
  (log/info (str "Setting up DB: " (:dbname db-spec)))
  (set-up-sqlite! db-spec))

(defmethod ig/halt-key! ::db
  [_ {:keys [datasource]}]
  ;; HikariCP pool is closeable
  (when datasource (.close datasource))
  (log/info "Discarding DB:" datasource)
  nil)
