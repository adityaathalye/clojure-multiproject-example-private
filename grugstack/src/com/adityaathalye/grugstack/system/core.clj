(ns com.adityaathalye.grugstack.system.core
  "OR pull in a library of pre-built Component components under the
  'system' directory. ORrrrr vendor the code for visibility. e.g. If we
  add danielsz/system as a git submodule (the only, and the only sane
  way to use submodules) under our 'system' directory, then BOOM, we
  have all the recipes. https://danielsz.github.io/system/"
  (:require
   [integrant.core :as ig]
   [ring.adapter.jetty]
   [com.adityaathalye.grugstack.settings.core :as settings])
  (:gen-class))

(def default-system-components
  {::components {:db-primary (ig/ref :com.adityaathalye.grugstack.system.db.primary.sqlite/db)
                 :db-sessions (ig/ref :com.adityaathalye.grugstack.system.db.sessions.sqlite/db)
                 :environment (ig/ref :com.adityaathalye.grugstack.system.runtime/environment)}})

#_(def build-config-map nil)
(defmulti build-config-map
  ::module)

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
  [{{:keys [system-modules]} ::settings
    components ::components
    :or {components default-system-components}
    :as settings}]
  (apply require (map symbol system-modules))
  (let [cfg (reduce (fn [system-configuration module-name]
                      (merge system-configuration
                             (build-config-map (assoc settings
                                                      ::module module-name))))
                    {}
                    system-modules)]
    (-> cfg
        (dissoc ::settings)
        (assoc ::components components)
        ig/expand)))

(defmethod ig/init-key ::components
  [_ system-map]
  system-map)

(defn init
  [settings]
  (let [cfg (expand settings)]
    (ig/load-namespaces cfg)
    (-> cfg ig/init)))

(comment

  (def test-system
    (do (when test-system (ig/halt! test-system))
        (init (settings/make-settings (settings/read-settings!
                                       "com/example/settings.edn")))))

  (expand
   (settings/make-settings (settings/read-settings!
                            "com/example/settings.edn"))))
