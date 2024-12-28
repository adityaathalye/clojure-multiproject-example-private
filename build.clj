(ns build
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.build.api :as b]))



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
