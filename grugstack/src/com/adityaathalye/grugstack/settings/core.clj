(ns com.adityaathalye.grugstack.settings.core
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [integrant.core :as ig])
  (:gen-class))

(def default-settings-edn-file
  "TODO: Write a specification for this file. Optionally use aero to manage it."
  "com/adityaathalye/grugstack/settings/defaults.edn")

(defn read-settings!
  [settings-file]
  (-> settings-file
      io/resource
      slurp
      #_edn/read-string
      ig/read-string))

(let [default-settings (read-settings! default-settings-edn-file)]
  (defn make-settings
    ([] (make-settings {}))
    ([custom-settings]
     (make-settings default-settings
                    custom-settings))
    ([base-settings custom-settings]
     {:post [(let [app-name (get-in % [:com.adityaathalye.grugstack.system.core/settings :app-name])]
               (and app-name
                    (not= app-name "OVERRIDE_ME_PLACEHOLDER")))]}
     (merge-with merge
                 base-settings
                 custom-settings))))

(comment

  ;; default config should force assert to fail
  (make-settings)

  ;; suitable override should succeed
  (make-settings {:app-name "custom-app-name"})

  )
