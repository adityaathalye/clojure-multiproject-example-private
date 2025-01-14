(ns usermanager.test-utilities
  (:require [usermanager.main :as um]
            [usermanager.system.core :as system]
            [clojure.string :as s]
            [usermanager.model.user-manager :as model]
            [clojure.java.io :as io]
            [usermanager.router.core :as router]
            [integrant.core :as ig]
            [com.adityaathalye.grugstack.settings.core :as grug-settings]
            [com.adityaathalye.grugstack.system.core :as grug-system])
  (:import (java.net ServerSocket)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Configuration utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-free-port!
  []
  (with-open [socket (ServerSocket. 0)]
    (.getLocalPort socket)))

(defn grug-test-settings
  ([]
   (grug-test-settings {}))
  ([ns-settings-overrides]
   (let [settings (grug-settings/make-settings
                   (grug-settings/read-settings! "usermanager/settings.edn")
                   {})
         common-test-settings {:com.adityaathalye.grugstack.system.core/settings
                               {:runtime-environment-type "test"}
                               :com.adityaathalye.grugstack.system.server-simple/jetty-adapter
                               {:port (get-free-port!)}}]
     (merge-with merge
                 settings
                 common-test-settings
                 ns-settings-overrides))))

#_(grug-test-settings
   {:com.adityaathalye.grugstack.system.server-simple/server
    {:handler-thunk `identity}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test fixture utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn setup-teardown-server!
  [{:keys [server-key config middleware-key middleware-stack]
    :or {server-key ::server
         middleware-key ::middleware
         middleware-stack []} :as settings}
   f]
  (let [config (merge {:port (get-free-port!) :join? false}
                      config)]
    (println "Setting up component:" server-key)
    (println "With settings:" settings)
    (system/set-config! server-key config)
    (system/set-config! middleware-key {:stack middleware-stack})
    (system/start-middleware-stack! middleware-key)
    (system/start-server!
     (um/wrap-router router/router middleware-key) server-key)

    (println "Running test with config:" (system/get-config server-key))
    (f)

    (system/stop-server! server-key)
    (system/evict-component! server-key)
    (system/evict-component! middleware-key)
    (println "Stopped and evicted components:" server-key middleware-key)))

(defn with-test-db
  [db-key f]
  (let [dbname (format "test/%s.sqlite3"
                       (-> db-key symbol str
                           (s/replace #"\.|/" "_")))]
    (println "Setting up component:" db-key)
    (system/set-config! db-key {:dbtype "sqlite" :dbname dbname})
    (system/start-db! model/populate db-key)

    (println "Running test with config:" (system/get-config db-key))
    (f)

    (system/stop-db! db-key)
    (system/evict-component! db-key)
    (println "Stopped and evicted component:" db-key)

    (try (io/delete-file dbname)
         (catch java.io.IOException e
           (println (ex-message e)))
         (finally (println "Deleted SQLite test DB:" dbname)))))

(defn setup-teardown-grug!
  ([f]
   (setup-teardown-grug! {} (atom nil) f))
  ([test-ns-system-atom f]
   (setup-teardown-grug! {} test-ns-system-atom f))
  ([test-ns-settings-overrides test-ns-system-atom f]
   (let [settings (grug-test-settings test-ns-settings-overrides)
         system (grug-system/init settings)
         dbname (get-in system
                        [:com.adityaathalye.grugstack.system.db.primary.sqlite/db
                         :dbname])]
     (reset! test-ns-system-atom system)
     (println "Setting up grug system:")
     (println "With settings:" settings)
     (f)
     (ig/halt! system)
     (reset! test-ns-system-atom nil)
     (try (io/delete-file dbname)
          (catch java.io.IOException e
            (println (ex-message e)))
          (finally (println "Deleted SQLite test DB:" dbname)))
     (println "Stopped grug system."))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HTTP utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn echo-response
  [method path]
  {:status 200
   :headers {"Content-Type" "text/plain;charset=utf-8"}
   :body (format "echoing METHOD %s for PATH %s"
                 method path)})

(def not-found-response
  {:status 404
   :headers {}
   :body "Not Found."})
