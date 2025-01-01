(ns build
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.build.api :as b]))

(defn make-opts [{:keys [lib main version basis-opts target-dir class-dir src-dirs ns-compile] :as opts
                  :or {lib 'com.example/example
                       main 'com.example.core
                       ns-compile '[com.example.core]
                       version "0.1.0-SNAPSHOT"
                       basis-opts {}
                       target-dir "target"
                       class-dir "target/classes"
                       src-dirs ["grugstack/src" "example_app/src"]}}]
  (assoc opts
         :uber-file (format "target/%s-%s.jar" lib version)
         :basis (b/create-basis basis-opts)
         :main main
         :ns-compile ns-compile
         :target-dir target-dir
         :class-dir class-dir
         :src-dirs src-dirs))

(defn test "Run all the tests."
  ([] (test (make-opts {})))
  ([{:keys [aliases]
     :or {aliases [:grugstack/grugstack :all/test]}
     :as opts}]
   (let [basis (b/create-basis {:aliases aliases})
         cmds (b/java-command
               {:basis basis
                :main 'clojure.main
                :main-args ["-m" "cognitect.test-runner"]})
         {:keys [exit]} (b/process cmds)]
     (when-not (zero? exit) (throw (ex-info "Tests failed" {}))))
   opts))

(defn build
  ([] (build (make-opts {})))
  ([{:keys [src-dirs class-dir target-dir main ns-compile] :as opts
     :or {target-dir "target"
          ns-compile '[com.example.core]
          class-dir "target/classes"
          src-dirs ["grugstack/src" "grugstack/resources" "tblm/mothra/src" "tblm/mothra/resources"]}}]
   (println "\nBuilding uberjar with opts: " (dissoc opts :basis))
   (println "\nCleaning build target directory...")
   (b/delete {:path target-dir})
   (println "\nCopying source...")
   (b/copy-dir {:src-dirs src-dirs
                :target-dir class-dir})
   (println (str "\nCompiling " main "..."))
   (b/compile-clj opts)
   (let [uber-opts (select-keys opts
                                [:class-dir :uber-file :basis :main]) ]
     (println "\nBuilding JAR... with uber-opts:" (dissoc uber-opts :basis))
     (b/uber uber-opts))
   opts))

(defn build-example-app
  [_]
  (-> {:basis-opts {:aliases [:grugstack/grugstack :com.example]}}
      make-opts
      build))

(comment

  (-> {:basis-opts {:aliases [:grugstack/grugstack :com.example]}}
      make-opts
      build)
  )

(defn ci "Run the CI pipeline of tests (and build the uberjar)."
  [{:keys [class-dir main] :as opts}]
  (test opts)
  (b/delete {:path "target"})
  (let [opts (make-opts opts)]
    (println "\nCopying source...")
    (b/copy-dir {:src-dirs ["resources" "src"] :target-dir class-dir})
    (println (str "\nCompiling " main "..."))
    (b/compile-clj opts)
    (println "\nBuilding JAR...")
    (b/uber opts))
  opts)

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
