(ns com.adityaathalye.grugstack.system.core
  "OR pull in a library of pre-built Component components under the
  'system' directory. ORrrrr vendor the code for visibility. e.g. If we
  add danielsz/system as a git submodule (the only, and the only sane
  way to use submodules) under our 'system' directory, then BOOM, we
  have all the recipes. https://danielsz.github.io/system/"
  (:require
   [integrant.core :as ig]
   [ring.adapter.jetty]
   [clojure.tools.logging :as log]
   [com.adityaathalye.grugstack.settings.core :as settings])
  (:gen-class))

(def system-map
  {::system {:db-primary (ig/ref [:com.adityaathalye.grugstack.system.sqlite/db
                                  :com.adityaathalye.grugstack.system.sqlite/primary])
             :db-sessions (ig/ref [:com.adityaathalye.grugstack.system.sqlite/db
                                   :com.adityaathalye.grugstack.system.sqlite/sessions])
             :environment (ig/ref :com.adityaathalye.grugstack.system.runtime/environment)}})

#_(def build-config-map nil)
(defmulti build-config-map
  ::module)

(defmethod build-config-map :default
  [config-map]
  (let [module (::module config-map)]
    (log/warn (format "No config. for module %s. Only its namespace will be loaded, to support symbol resolution." (symbol module)))
    config-map
    ;; (require (symbol module))
    #_(try
         (catch java.io.FileNotFoundException _)
         (finally (log/warn "Could not find file for namespace of module: " module)))))

(defmethod ig/init-key ::settings
  [_ {:keys [app-name runtime-environment-type]
      :as settings}]
  (assoc settings
         :app-name (name app-name)
         :runtime-environment (name runtime-environment-type)))

(defn expand
  "The way init works, the ::settings map never appears in the
   config. This is intentional. Settings can contain secrets that we
   don't want to have to remember to elide from the final configuration."
  ([settings] (expand settings system-map))
  ([{{:keys [system-modules]} ::settings
     :as settings}
    system-map]
   (apply require (map symbol system-modules))
   (let [cfg (reduce (fn [system-configuration module-name]
                       (merge system-configuration
                              (build-config-map (assoc settings
                                                       ::module module-name))))
                     {}
                     system-modules)]
     (-> cfg
         (dissoc ::settings)
         (merge system-map)
         ig/expand))))

(defmethod ig/init-key ::system
  [_ system-map]
  system-map)

(defn init
  [settings]
  (let [cfg (expand settings)]
    (log/info "loading namespaces" (keys cfg))
    (ig/load-namespaces cfg)
    (-> cfg ig/init)))


(comment

  (init (settings/make-settings (settings/read-settings!
                                 "com/example/settings.edn")))

  )
