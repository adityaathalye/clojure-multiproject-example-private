(ns com.bombaylitmag.handlers.core
  (:require
   [clojure.tools.logging :as log]
   [com.bombaylitmag.handlers.common
    :as handlers-common
    :refer [html-fragment html5-page]]
   [com.bombaylitmag.middleware.core :as middleware-core]
   [reitit.coercion.malli]
   [reitit.ring]
   [reitit.ring.middleware.parameters :as rrm-params]
   [ring.middleware.keyword-params :as rmk-params]
   [ring.util.response :as res]))

(def reitit-route-tree
  ["" {:handler html-fragment} ; return hypermedia fragment by default
   ["/" {:get {:handler ^:replace html5-page
               :mothra/view {:template :default
                             :page-name "Home Page"}}}]
   ["/echo" {:get {:mothra/view {:template :echo}}}]
   ["/foo" {:get {:mothra/view {:template :foo}}}]])

(defn ring-routes
  []
  (reitit.ring/routes
   (reitit.ring/redirect-trailing-slash-handler)
   ;; static assets nested under "resources/public"
   (reitit.ring/create-resource-handler {:path "/"})
   (reitit.ring/create-default-handler
    {:not-found (constantly (res/not-found "Not found."))})))

(defmulti router
  :env)

(defmethod router :default
  [system]
  ;; Assume dev environment if no known env specified.
  (router (assoc system :env :dev)))

(defmethod router :dev
  [system]
  (reitit.ring/router
   reitit-route-tree
   {:data {:system system
           :coercions reitit.coercion.malli/coercion
           :middleware [middleware-core/wrap-view-ctx
                        rrm-params/parameters-middleware
                        rmk-params/wrap-keyword-params
                        middleware-core/wrap-system
                        #_[cors/wrap-cors :access-control-allow-origin #".*"
                           :access-control-allow-methods [:get :put :post :patch :delete]]]}}))

(defmulti app
  "Given a well-formed system definition, create a Ring app for the system's :runtime/env.

  TODO: Consider using Integrant's Profiles to drive this."
  :env)

(defmethod app :default
  [system]
  (log/info "Calling app :default method with config (or system?) " system)
  (reitit.ring/ring-handler
   (router system)
   (ring-routes)))

(defmethod app :dev
  [system]
  (log/info "Calling app :dev method with config (or system?) " system)
  (reitit.ring/reloading-ring-handler
   #(reitit.ring/ring-handler
     (router system)
     (ring-routes))))

(comment
  (do
    (require 'reitit.core)

    (def wrap-application-view
      {:name ::app-view
       :compile (fn [data _]
                  (fn [handler]
                    (fn [req]
                      (println data)
                      (handler (merge req
                                      (select-keys data [:mothra/view]))))))})

    (def ring-router
      (reitit.ring/router
       ["" {}
        ["/api/ping" ::ping]
        ["/api/orders" {:middleware [wrap-application-view]
                        :get (fn [request]
                               {:status 200
                                :body (:mothra/view request "Not Found.")})
                        :mothra/view "bar"
                        :name ::foo}]]))

    (def app-try
      (reitit.ring/ring-handler
       ring-router)))

  (reitit.core/match-by-path ring-router
                             "/api/orders")

  (app-try {:request-method :get :uri "/api/orders"})

  (-> app-try (reitit.ring/get-router) (reitit.core/match-by-name ::foo))

  )
