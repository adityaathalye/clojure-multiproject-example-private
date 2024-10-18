(ns user
  (:require [integrant.repl :as ig-repl]
            [clojure.tools.namespace.repl :as repl]
            [com.bombaylitmag.system.core]
            [clojure.repl.deps :as repl-deps :refer [add-lib]]))

(ig-repl/set-prep! (constantly (com.bombaylitmag.system.core/config :dev)))

;; ref: https://ryanmartin.me/articles/clojure-fly/
(repl/set-refresh-dirs "src" "resources")

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
(def reset-all ig-repl/reset-all)

(comment
  (go)
  (halt)
  (reset)
  (reset-all)

  (add-lib 'sym {:mvn/version "x.y.z"})
  )
