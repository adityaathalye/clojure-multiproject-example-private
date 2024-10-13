(ns com.adityaathalye.grugstack.system.server
  (:require
   [com.adityaathalye.grugstack.system.core :as system]
   [com.adityaathalye.grugstack.system.application :as system-application]
   [integrant.core :as ig]
   [clojure.tools.logging :as log]
   [ring.adapter.jetty])
  (:gen-class))

(defmethod system/build-config-map :com.adityaathalye.grugstack.system.server
  [{{:keys [type port join?]
     :or {type :jetty port 13337 join? false}
     :as adapter-settings} ::jetty-adapter}]
  (log/info (format "Configured adapter %s at port %s with adapter-settings %s" type port adapter-settings))
  {::jetty-adapter (merge {:type type :port port :join? join?}
                          adapter-settings)
   ::server {:adapter-config (ig/ref ::jetty-adapter)
             :system (ig/ref ::system/system)
             :reitit-route-tree (ig/ref ::system-application/reitit-route-tree)
             :handler (ig/ref ::system-application/handler)}})

(defmethod ig/init-key ::jetty-adapter
  [_ adapter-options]
  adapter-options)

(defmethod ig/init-key ::server
  [_ {:keys [adapter-config system reitit-route-tree handler] :as server}]
  (assoc server
         :object
         (case (:type adapter-config)
           :jetty (ring.adapter.jetty/run-jetty
                   (handler {:system system
                             :reitit-route-tree reitit-route-tree})
                   (-> adapter-config
                       (dissoc :handler)
                       (assoc :join? false)))
           nil)))

(defmethod ig/resolve-key ::server
  [_ {:keys [object]}]
  object)

(defmethod ig/halt-key! ::server
  [_ {:keys [object] :as _server}]
  (log/info "Stopping Jetty server.")
  (when object
    (.stop object))
  nil)
