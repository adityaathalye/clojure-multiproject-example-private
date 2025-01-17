(ns build
  "Provide commands like `test`, `uberjar`, `ci`, to invoke this way:

      clj -T:root/build :app-alias ':com.acmecorp.snafuapp.core'

  COMMAND OPTIONS

  A single options map is passed to all top-level commands.

  `make-opts` is the constructor function that produces a well-formed
  options map. It ensures sane defaults that follow our multiproject
  conventions. All commands use to ensure they use a well-formed opts
  map. It constructs defaults. It passes through options provided at
  the command line.

  Options to pass at the command line:

  `:app-alias` (optional, keyword): e.g. `:com.example.core`. Narrow
  scope of command invoked, to the matching alias found in the
  multiproject's root `deps.edn`.

  `:ns-compile` (optional, vector of symbols). All the namespaces to
  compile in advance. When provided, it *must* contain the
  application's main entry point. When NOT provided, `make-opts`
  constructs it using the entry point namespace named by `:app-alias`.

  IMPORTANT MULTIPROJECT CONVENTIONS:

  `:app-alias` naming convention does a lot of work for us.

  - It *must* be a keyword. The name must match one of the application
  aliases found in deps.edn. By Grug-Brain convention, this keyword
  *must* be the same as the aliased application's main entry point
  namespace, as defined under the deps alias.

  The build script relies on our namespacing convention to ensure
  separation of build-time classpaths, intermediate artifacts, and so
  forth."
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.build.api :as b])
  (:import [java.util Date TimeZone]
           [java.text SimpleDateFormat]))

(defn- timestamp
  "Copied from yogthos/migratus."
  []
  (let [fmt (doto (SimpleDateFormat. "yyyyMMddHHmmss")
              (.setTimeZone (TimeZone/getTimeZone "UTC")))]
    (.format fmt (Date.))))

(defn- git-short-hash
  []
  (b/git-process {:git-args "rev-parse --short HEAD"}))

(defn create-app-basis
  [{:keys [app-alias aliases] :as _opts}]
  (println "CREATING BASIS:" {:aliases (conj aliases app-alias)})
  (b/create-basis {:aliases (conj aliases app-alias)}))

(defn basis->src-dirs
  [{:keys [libs argmap] :as _basis}]
  (let [lib-names (-> argmap :extra-deps keys)]
    (mapcat #(get-in libs [% :paths])
            lib-names)))

(defn uber-file-name
  [target-dir app-name git-sha timestamp]
  (format "%s/%s-%s-%s.jar"
          target-dir
          app-name
          git-sha
          timestamp))

(comment
  (mapv #(get-in (create-app-basis {:app-alias :com.acmecorp.snafuapp.snafuapp})
                 %)
        [[:argmap]
         [:libs 'acmecorp/snafuapp :paths]
         [:libs 'parts/grugstack :paths]]))

(defn make-opts
  [{:keys [app-alias ns-compile]
    :or {app-alias :com.example.core}
    :as opts}]
  (let [basis (create-app-basis opts)
        app-alias (name app-alias)
        target-dir (format "target/%s" app-alias)
        class-dir (format "%s/classes" target-dir)]
    (assoc opts
           :uber-file (uber-file-name target-dir
                                      app-alias
                                      (git-short-hash)
                                      (timestamp))
           :basis basis
           :lib (symbol app-alias)
           :main (symbol app-alias)
           :ns-compile (mapv symbol
                             (if (empty? ns-compile)
                               [app-alias] ns-compile))
           :target-dir target-dir
           :class-dir class-dir
           :src-dirs (basis->src-dirs basis))))

(defn test "Run all the tests."
  [opts]
  (println "TESTING with input opts" opts)
  (let [opts (-> opts
                 (assoc :aliases [:root/all :root/test])
                 make-opts)
        test-dirs (interleave (repeat "--dir")
                              (get-in opts [:basis :argmap :exec-args :dirs]))
        _ (println "TEST DIRS " test-dirs)
        cmds (b/java-command
              {:basis (:basis opts)
               :main 'clojure.main
               :main-args (into ["-m" "cognitect.test-runner"] test-dirs)})
        {:keys [exit]} (b/process cmds)]
    (when-not (zero? exit) (throw (ex-info "Tests failed" {})))
    opts))

(defn uberjar
  [opts]
  (let [{:keys [src-dirs target-dir main]
         :as opts} (make-opts opts)]
    (println "\nBuilding uberjar with opts: " (dissoc opts :basis))
    (println "\nCleaning this build's target directory..." target-dir)
    (b/delete {:path target-dir})
    (println "\nCopying source to target..." {:src-dirs src-dirs :target-dir target-dir})
    (b/copy-dir {:src-dirs src-dirs
                 :target-dir target-dir})
    (println (str "\nCompiling " main "..."))
    (b/compile-clj opts)
    (let [uber-opts (select-keys opts
                                 [:class-dir :uber-file :basis :main])]
      (println "\nBuilding JAR... with uber-opts:" (dissoc uber-opts :basis))
      (b/uber uber-opts))
    opts))

(defn ci "Run the CI pipeline of tests (and build the uberjar)."
  [opts]
  (test opts)
  (uberjar opts)
  opts)

(defn debug-echo-opts-map
  [{:keys [a b]
    :or {a 1 b 2}
    :as opts}]
  (println (assoc opts))
  opts)

(comment

  (map (comp sort keys b/create-basis) [{:aliases [:foo]}
                                        {:aliases [:grugstack]}
                                        {:aliases [:grugstack
                                                   :root/dev
                                                   :root/test
                                                   :root/run-x]}])
  (tap> (b/create-basis {:aliases []}))

  (tap> (b/create-basis {:aliases [:root/build]}))

  (tap> (b/create-basis {:aliases [:grugstack :grugalias/foo]}))

  (tap> (b/create-basis {:aliases [:grugstack
                                   :root/dev
                                   :root/test
                                   :root/run-x]})))
