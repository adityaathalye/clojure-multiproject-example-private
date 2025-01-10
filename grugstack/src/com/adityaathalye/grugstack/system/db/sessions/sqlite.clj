(ns com.adityaathalye.grugstack.system.db.sessions.sqlite
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [com.adityaathalye.grugstack.system.core :as system]
   [com.adityaathalye.grugstack.system.db.sqlite :as sqlite])
  (:gen-class))

(defmethod system/build-config-map :com.adityaathalye.grugstack.system.db.sessions.sqlite
  [{{:keys [app-name runtime-environment-type]}
    :com.adityaathalye.grugstack.system.core/settings
    {:keys [dbname dbpath migrator db-spec]
     :or {migrator identity}}
    :com.adityaathalye.grugstack.system.db.sessions.sqlite/db}]
  ;; (log/info "Configuring module" *ns*)
  {::db {:dbname (or dbname
                     (format "%s_%s_sessions.sqlite3" app-name runtime-environment-type))
         :db-spec (or db-spec
                      (ig/ref ::sqlite/db-spec))
         :migrator migrator
         :datasource nil}})

(defmethod ig/init-key ::db
  [_ {:keys [dbname db-spec migrator]}]
  (log/info (str "Setting up DB: " dbname))
  (let [datasource (sqlite/set-up! dbname db-spec)]
    (when migrator
      (log/info "Migrating DB:" dbname)
      (with-open [connection (jdbc/get-connection datasource)]
        (migrator connection)))
    {:dbname dbname
     :migrator migrator
     :datasource datasource}))

(defmethod ig/resolve-key ::db
  [_ {:keys [datasource]}]
  datasource)

(defmethod ig/halt-key! ::db
  [_ {:keys [datasource]}]
  ;; Datasource must be a closeable (e.g. Hikari connection pool)
  (when datasource
    (.close datasource))
  (log/info "Discarding DB:" datasource)
  nil)
