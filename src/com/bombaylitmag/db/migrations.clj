(ns com.bombaylitmag.db.migrations
  (:require [com.bombaylitmag.db.utils :as db-utils]))

(def sql-queries
  (db-utils/sql-queries-map-init!
   "com/bombaylitmag/db"
   {::pragmas "pragmas.sql"
    ::ddl "model.sql"}))

(defn migrate!
  []
  (::ddl sql-queries))
