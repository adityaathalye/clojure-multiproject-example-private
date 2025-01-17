(ns grugstack.utilities.integrant-repl
  (:require [integrant.repl :as ig-repl]
            [integrant.repl.state :as ig-state]
            [clojure.tools.namespace.repl :as repl]
            [clojure.repl.deps :as repl-deps :refer [add-lib]]
            [portal.api :as p]
            [clojure.reflect :as reflect]
            [com.adityaathalye.grugstack.settings.core :as settings]
            [com.adityaathalye.grugstack.system.core :as system]))

(defn set-project-integrant!
  [namespace-sym]
  (ig-repl/set-prep!
   #(system/expand (settings/make-settings
                    (settings/read-settings! "com/acmecorp/snafuapp/settings.edn"))))

  ;; ref: https://ryanmartin.me/articles/clojure-fly/
  (repl/set-refresh-dirs "src" "resources" "parts")

  (def go ig-repl/go)
  (def halt ig-repl/halt)
  (def reset ig-repl/reset)
  (def reset-all ig-repl/reset-all))

(comment
  (go)
  (halt)
  (reset)
  (reset-all)

  ig-state/system

  (do ;; open fresh portal
    (do ;; clean stop
      (p/clear)
      (p/close))
    (do ;; reopen and tap
      (p/open)
      (add-tap #'p/submit)))

  (tap> ig-state/system)

  (p/clear)

  #_(p/start)

  (tap> (.getConnection (get-in ig-state/system [:database/primary :reader])))

  (add-lib 'sym {:mvn/version "x.y.z"}))
