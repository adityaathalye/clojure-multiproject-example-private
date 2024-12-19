(ns com.bombaylitmag.handlers.core
  (:require
   [com.bombaylitmag.handlers.common
    :as handlers-common
    :refer [html-fragment html5-page]]
   [com.bombaylitmag.middleware.core :as middleware-core]
   [reitit.ring]))

(def reitit-route-tree
  ["" {:handler html-fragment} ; return hypermedia fragment by default
   ["/" {:get {:handler ^:replace html5-page
               :mothra/view {:template :default
                             :page-name "Home Page"}}}]
   ["/echo" {:get {:mothra/view {:template :echo}}}]
   ["/foo" {:get {:mothra/view {:template :foo}}}]])
