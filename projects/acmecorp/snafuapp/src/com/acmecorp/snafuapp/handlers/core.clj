(ns com.acmecorp.snafuapp.handlers.core
  (:require
   [com.acmecorp.snafuapp.handlers.common
    :as handlers-common
    :refer [html-fragment html5-page]]
   [reitit.ring]
   [com.adityaathalye.grugstack.system.core :as system]))

(def reitit-route-tree
  ["" {:handler html-fragment} ; return hypermedia fragment by default
   ["/" {:get {:handler ^:replace html5-page
               :snafuapp/view {:template :default
                               :page-name "Home Page"}}}]
   ["/echo" {:get {:snafuapp/view {:template :echo}}}]
   ["/foo" {:get {:snafuapp/view {:template :foo}}}]])

(defmethod system/build-config-map :com.acmecorp.snafuapp.handlers.core
  [{:com.adityaathalye.grugstack.system.application/keys [reitit-route-tree]
    :or {reitit-route-tree 'com.acmecorp.snafuapp.handlers.core/reitit-route-tree}}]
  {:com.adityaathalye.grugstack.system.application/reitit-route-tree reitit-route-tree})
