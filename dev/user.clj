(ns user
  (:require [integrant.repl :as ig-repl]
            [integrant.repl.state :as ig-state]
            [clojure.tools.deps :as deps]
            [clojure.tools.namespace.repl :as repl]
            [clojure.repl.deps :as repl-deps :refer [add-lib sync-deps]]
            [portal.api :as p]
            [com.adityaathalye.grugstack.settings.core :as settings]
            [com.adityaathalye.grugstack.system.core :as system]))

(defn set-prep!
  [settings-file-grug-style]
  (ig-repl/set-prep!
   #(system/expand (settings/make-settings
                    (settings/read-settings! settings-file-grug-style)))))

(defn go [] (ig-repl/go))
(defn halt [] (ig-repl/halt))
(defn reset [] (ig-repl/reset))
(defn reset-all [] (ig-repl/reset-all))

;; ref: https://ryanmartin.me/articles/clojure-fly/
(repl/set-refresh-dirs "src" "resources" "grugstack" "projects")

(comment
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; SET UP SYSTEM HELPERS
;;;
;;; Call set-prep manually, depending on project REPL
;;; connected to, before using the "SYSTEM helpers"
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (set-prep! "FIXME/settings.edn")

  (set-prep! "com/example/settings.edn")

  (set-prep! "com/acmecorp/snafuapp/settings.edn")

  (set-prep! "usermanager/settings.edn")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; SYSTEM helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (go)
  (halt)
  (reset)
  (reset-all)

  ig-state/system

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; VISUAL tools
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  (do ;; open fresh portal
    (do ;; clean stop
      (p/clear)
      (p/close))
    (do ;; reopen and tap
      (p/open)
      (add-tap #'p/submit)))

  ;; Visualise
  (tap> ig-state/system)

  (p/clear)

  #_(p/start)

  (tap> (.getConnection (get-in ig-state/system [:database/primary :reader])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; DEPS helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  ;; Explore the deps tree
  (:project-edn (deps/find-edn-maps "./deps.edn"))

  ;; Try a lib
  (add-lib 'sym {:mvn/version "x.y.z"})

  ;; Update libs from deps.edn
  (sync-deps)

  0) ;; END OF COMMENT BLOCK
