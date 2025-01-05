(ns com.bombaylitmag.db.migrations
  (:require [com.bombaylitmag.db.utils :as db-utils :refer [reader writer]]
            [next.jdbc :as jdbc]
            [clojure.tools.logging :as log]))

(def sql-queries
  (db-utils/sql-queries-map-init!
   "com/bombaylitmag/db"
   {::ddl "model.sql"}))

(defn migrate!
  [ds]
  (with-open [ds (writer ds)]
    (log/info "MIGRATING DDL" (select-keys sql-queries [::ddl]))
    (log/info "DB connection is: " ds)
    ds))
