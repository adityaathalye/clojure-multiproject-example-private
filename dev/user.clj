(ns user
  (:require [integrant.repl :as ig-repl]
            [integrant.repl.state :as ig-state]
            [clojure.tools.namespace.repl :as repl]
            [clojure.repl.deps :as repl-deps :refer [add-lib]]
            [portal.api :as p]
            [clojure.reflect :as reflect]
            [settings.core :as settings]
            [system.core :as system]))

(ig-repl/set-prep!
 (fn []
   (system/init (settings/make-settings
                 (settings/read-settings! "settings/com/bombaylitmag/settings.edn")))))

;; ref: https://ryanmartin.me/articles/clojure-fly/
(repl/set-refresh-dirs "src" "resources" "grugstack")

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
(def reset-all ig-repl/reset-all)

(comment
  (go)
  (halt)
  (reset)
  (reset-all)

  (reflect/reflect ig-state/system)

  ;; Open portal
  (p/open)
  (add-tap #'p/submit)

  (p/stop)
  #_(p/start)

  (tap> ig-state/system)

  (tap> (.getConnection (get-in ig-state/system [:database/primary :reader])))

  (add-lib 'sym {:mvn/version "x.y.z"})
  )
