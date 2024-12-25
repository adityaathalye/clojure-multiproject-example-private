(ns com.bombaylitmag.handlers.core
  (:require
   [com.bombaylitmag.handlers.common
    :as handlers-common
    :refer [html-fragment html5-page]]
   [com.bombaylitmag.middleware.core :as middleware-core]
   [reitit.ring]
   [system.core :as system]))

(def reitit-route-tree
  ["" {:handler html-fragment} ; return hypermedia fragment by default
   ["/" {:get {:handler ^:replace html5-page
               :mothra/view {:template :default
                             :page-name "Home Page"}}}]
   ["/echo" {:get {:mothra/view {:template :echo}}}]
   ["/foo" {:get {:mothra/view {:template :foo}}}]])

(defmethod system/build-config-map :com.bombaylitmag.handlers.core
  [{:system.application/keys [reitit-route-tree]
    :or {reitit-route-tree 'com.bombaylitmag.handlers.core/reitit-route-tree}}]
  {:system.application/reitit-route-tree reitit-route-tree})
