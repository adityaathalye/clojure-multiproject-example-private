(ns system.application
  (:require
    [integrant.core :as ig]
    [clojure.tools.logging :as log]
    [system.core :as system])
  (:gen-class))

(defmethod system/build-config-map :system.application
  [_]
  {::middleware [(fn [_]
                   (assert "Middleware absent. Please define explicitly."))]
   ::handler (fn [_request]
               {:status 200
                :body "Dummy handler. Please override me at system start."})
   ::server {:server/type :jetty
             :server/port 3000
             :server/app (ig/ref ::handler)}})

(comment
  (system/build-config-map {:system.core/module :system.application})
  )

(defmethod ig/init-key ::middleware
  [_ middleware-stack]
  middleware-stack)

(defmethod ig/init-key ::handler
  [_ handler]
  handler)

(defmethod ig/init-key ::server
  [_ server-config]
  (log/info server-config)
  server-config)
