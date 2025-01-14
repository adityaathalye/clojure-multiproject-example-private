(ns com.acmecorp.snafuapp.db.utils
  (:require [clojure.java.io :as io]
            [java-time.api :as jt])
  (:import [java.util Date TimeZone]
           [java.text SimpleDateFormat]))

(defprotocol IDatabaseConnectionPool
  (reader [pool])
  (writer [pool])
  (readwrite [pool]))

(defmulti connection-pool)

(defn timestamp!
  "Copied from yogthos/migratus."
  []
  (let [fmt (doto (SimpleDateFormat. "yyyyMMddHHmmss")
              (.setTimeZone (TimeZone/getTimeZone "UTC")))]
    (.format fmt (Date.))))

(defn init-query-file!
  "Initialise a new query file if it does not already exist."
  [path query-name]
  (let [query-file (format "%s/QUERY_%s_%s.sql"
                           path
                           query-name
                           (timestamp!))]
    (when-not (io/resource query-file)
      (spit (io/file query-file) ""))))

(comment
  (init-query-file! "com/acmecorp/snafuapp/db"
                    "foo"))

(defn sql-slurp!
  [sql-dir sql-query-file]
  (->> sql-query-file
       (str sql-dir "/")
       io/resource
       slurp))

(defn sql-queries-map-init!
  [sql-dir sql-query-map]
  (reduce-kv (fn [queries-map query-key query-file]
               (assoc queries-map
                      query-key
                      [(sql-slurp! sql-dir query-file)]))
             {}
             sql-query-map))
