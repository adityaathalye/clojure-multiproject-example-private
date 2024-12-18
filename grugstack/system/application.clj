(ns system.application
  (:require [integrant.core :as ig]
            [system.core :as system]
            [clojure.tools.logging :as log])
  (:gen-class))

(defmethod system/build-config-map :system.application
  [{:system.application/keys [middleware handler server]}]
  {::middleware (or (resolve (symbol middleware))
                    (fn [_handler]
                      (fn [_request]
                        (assert "Middleware absent. Please define explicitly."))))
   ::handler (or (resolve (symbol handler))
                 (fn [_request]
                   {:status 200
                    :body "Dummy handler. Please override me at system start."}))
   ::server (merge {:type :jetty
                    :port 1337
                    :app (ig/ref ::handler)}
                   server)})

(comment
  (system/build-config-map {:system.core/module :system.application
                            ::middleware 'identity
                            ::handler 'identity})
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
