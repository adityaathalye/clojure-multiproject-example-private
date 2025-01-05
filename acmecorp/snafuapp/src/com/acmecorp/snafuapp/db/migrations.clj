(ns com.acmecorp.snafuapp.db.migrations
  (:require [com.acmecorp.snafuapp.db.utils :as db-utils :refer [reader writer]]
            [next.jdbc :as jdbc]
            [clojure.tools.logging :as log]))

(def sql-queries
  (db-utils/sql-queries-map-init!
   "com/acmecorp/snafuapp/db"
   {::ddl "model.sql"}))

(defn migrate!
  [ds]
  (with-open [ds (writer ds)]
    (log/info "MIGRATING DDL" (select-keys sql-queries [::ddl]))
    (log/info "DB connection is: " ds)
    ds))
