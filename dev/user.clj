(ns user
  (:require [integrant.repl :as ig-repl]
            [integrant.repl.state :as ig-state]
            [clojure.main]
            [clojure.tools.deps :as deps]
            [clojure.tools.namespace.repl :as repl]
            [clojure.repl.deps :as repl-deps :refer [add-lib sync-deps]]
            [portal.api :as p]
            [com.adityaathalye.grugstack.settings.core :as settings]
            [com.adityaathalye.grugstack.system.core :as system]
            [nrepl.cmdline]))

(defn multiproject-repl-prompt
  [_]
  (let [[socket repl-ailas] (drop-while #(not= % "--socket")
                                        (clojure.main/with-bindings *command-line-args*))
        project-ctx (when socket (str socket " " repl-ailas))]
    (printf "\n[This REPL | %s]\n\\_%s=> "
            project-ctx
            (ns-name *ns*))))

(defn run-repl
  "TODO: UGLY HACK. Find standard tool alternative to this silly
  :prompt injection (hehe :)) into nrepl.cmdline's private `run-repl`.

  I want to be aware at all times, which REPL context I am in, so I am
  confident about evaluating (set-prep!) etc.

  Ideally, I want the REPL prompt to be set to the project alias.

  However, `(clojure.main/repl :prompt multiproject-repl-prompt)` starts a
  sub-repl, which messes with my preferred method of starting a REPL
  at a named unix domain file socket. If user.clj starts a sub-repl, then
  it swallows the file socket, and I can no longer locate it anywhere.

  I could not figure out how to set the prompt, from Cider, or nREPL, or
  clojure.main."
  ([{:keys [server options] :as repl-ctx}]
   ;; Since it is a pass-through wrapper over the private function, I
   ;; guess it will break only if maintainers add / modify the arity.
   (#'nrepl.cmdline/run-repl (assoc-in repl-ctx
                                       [:options :prompt]
                                       multiproject-repl-prompt)))
  ([host port]
   (run-repl host port nil))
  ([host port options]
   (run-repl {:server  (cond-> {}
                         host (assoc :host host)
                         port (assoc :port port))
              :options options})))

(defn whereami?
  []
  (drop-while #(not= % "--socket")
              (clojure.main/with-bindings
                *command-line-args*)))

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
(repl/set-refresh-dirs "src" "resources" "parts" "projects")

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
