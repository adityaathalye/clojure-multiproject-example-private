(ns com.adityaathalye.grugstack.system.db.primary.sqlite
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [com.adityaathalye.grugstack.system.core :as system]
   [com.adityaathalye.grugstack.system.db.sqlite :as sqlite]
   [next.jdbc :as jdbc])
  (:gen-class))

(defmethod system/build-config-map :com.adityaathalye.grugstack.system.db.primary.sqlite
  [{{:keys [app-name runtime-environment-type]}
    :com.adityaathalye.grugstack.system.core/settings
    {:keys [dbname migrator db-spec]}
    :com.adityaathalye.grugstack.system.db.primary.sqlite/db}]
  ;; (log/info "Configuring module" *ns*)
  {::db {:dbname (or dbname
                     (format "%s_%s_primary.sqlite3" app-name runtime-environment-type))
         :db-spec (or db-spec (ig/ref ::sqlite/db-spec))
         :migrator (when migrator (resolve migrator))
         :runtime-environment (ig/ref :com.adityaathalye.grugstack.system.runtime/environment)
         :datasource nil}})

(defmethod ig/init-key ::db
  [_ {:keys [dbname db-spec migrator]}]
  {:pre [(var? migrator)]}
  (log/info (str "Setting up DB: " dbname))
  (let [datasource (sqlite/set-up! dbname db-spec)]
    (log/info "Migrating DB:" dbname)
    (with-open [connection (jdbc/get-connection datasource)]
      (migrator connection))
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
