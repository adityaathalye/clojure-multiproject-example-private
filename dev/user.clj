(ns user
  (:require [integrant.repl :as ig-repl]
            [integrant.repl.state :as ig-state]
            [clojure.tools.namespace.repl :as repl]
            [clojure.repl.deps :as repl-deps :refer [add-lib sync-deps]]
            [portal.api :as p]
            [clojure.reflect :as reflect]
            [com.adityaathalye.grugstack.settings.core :as settings]
            [com.adityaathalye.grugstack.system.core :as system]))

(ig-repl/set-prep!
 #(system/expand (settings/make-settings
                  (settings/read-settings! "com/acmecorp/snafuapp/settings.edn"))))

;; ref: https://ryanmartin.me/articles/clojure-fly/
(repl/set-refresh-dirs "src" "resources" "grugstack")

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
(def reset-all ig-repl/reset-all)

(comment

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

  ;; Try a lib
  (add-lib 'sym {:mvn/version "x.y.z"})

  ;; Update libs from deps.edn
  (sync-deps)) ;; END OF COMMENT BLOCK
