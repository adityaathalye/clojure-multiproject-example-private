(ns com.bombaylitmag.db.migrations
  (:require [com.bombaylitmag.db.utils :as db-utils]
            [next.jdbc :as jdbc]
            [clojure.tools.logging :as log]))

(def sql-queries
  (db-utils/sql-queries-map-init!
   "com/bombaylitmag/db"
   {::ddl "model.sql"}))

(defn migrate!
  [ds]
  (with-open [ds (jdbc/get-connection ds)]
    (log/info "MIGRATING DDL" (select-keys sql-queries [::ddl]))
    ds))
