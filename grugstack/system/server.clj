(ns system.server
  (:require
    [system.core :as system]
    [integrant.core :as ig])
  (:gen-class))

(defmethod system/build-config-map :system.server
  [_]
  {::jetty nil})

(defmethod ig/init-key ::jetty
  [_ _args]
  :INITIALISED)
