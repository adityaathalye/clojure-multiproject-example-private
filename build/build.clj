(ns build
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.build.api :as b]
            [clojure.string :as string])
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
  (mapv #(get-in (create-app-basis {:app-alias :com.bombaylitmag.mothra})
            %)
        [[:argmap]
         [:libs 'tblm/mothra :paths]
         [:libs 'grugstack/grugstack :paths]])
  )

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
  (let [opts (make-opts opts)
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

(defn build
  [opts]
  (let [{:keys [src-dirs class-dir target-dir main]
         :as opts} (make-opts opts)]
    (println "\nBuilding uberjar with opts: " (dissoc opts :basis))
    (println "\nCleaning build target directory...")
    (b/delete {:path target-dir})
    (println "\nCopying source...")
    (b/copy-dir {:src-dirs src-dirs
                 :target-dir target-dir})
    (println (str "\nCompiling " main "..."))
    (b/compile-clj opts)
    (let [uber-opts (select-keys opts
                                 [:class-dir :uber-file :basis :main]) ]
      (println "\nBuilding JAR... with uber-opts:" (dissoc uber-opts :basis))
      (b/uber uber-opts))
    opts))

(defn ci "Run the CI pipeline of tests (and build the uberjar)."
  [opts]
  #_(build opts)
  (test opts)
  opts)

(defn foo
  [{:keys [a b]
    :or {a 1 b 2}
    :as context}]
  (println (assoc context :a a :b b))
  context)

(comment


  (map (comp sort keys b/create-basis) [{:aliases [:foo]}
                                        {:aliases [:grugstack/grugstack]}
                                        {:aliases [:grugstack/grugstack
                                                   :all/dev
                                                   :all/test
                                                   :all/run-x]}])
  (tap> (b/create-basis {:aliases []}))

  (tap> (b/create-basis {:aliases [:all/build]}))

  (tap> (b/create-basis {:aliases [:grugstack/grugstack :grugalias/foo]}))

  (tap> (b/create-basis {:aliases [:grugstack/grugstack
                                   :all/dev
                                   :all/test
                                   :all/run-x]}))
  )
