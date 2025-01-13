(ns com.adityaathalye.grugstack.system.server-simple
  (:require
   [com.adityaathalye.grugstack.system.core :as system]
   [integrant.core :as ig]
   [clojure.tools.logging :as log]
   [ring.adapter.jetty])
  (:gen-class))

(defmethod system/build-config-map :com.adityaathalye.grugstack.system.server-simple
  [{{:keys [type port join?]
     :or {type :jetty port 13337 join? false}
     :as adapter-settings} ::jetty-adapter
    {:keys [handler-thunk]} ::server}]
  (log/info (format "Configured adapter %s at port %s with adapter-settings %s" type port adapter-settings))
  {::jetty-adapter (merge {:type type :port port :join? join?}
                          adapter-settings)
   ::server {:adapter-config (ig/ref ::jetty-adapter)
             :components (ig/ref ::system/components)
             :handler-thunk handler-thunk}})

(defmethod ig/init-key ::jetty-adapter
  [_ adapter-options]
  adapter-options)

(defmethod ig/init-key ::server
  [_ {:keys [adapter-config components handler-thunk]
      :as server}]
  (if-let [handler ((resolve handler-thunk) components)]
    (assoc server
           :object
           (case (:type adapter-config)
             :jetty (ring.adapter.jetty/run-jetty
                     handler
                     (-> adapter-config
                         (assoc :join? false)))
             nil))
    (log/info "NULL HANDLER!!!!!!!! for"
              (resolve handler-thunk))))

(defmethod ig/resolve-key ::server
  [_ {:keys [object]}]
  object)

(defmethod ig/halt-key! ::server
  [_ {:keys [object] :as _server}]
  (log/info "Stopping Jetty server.")
  (when object
    (.stop object))
  nil)
