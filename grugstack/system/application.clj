(ns system.application
  (:require [integrant.core :as ig]
            [system.core :as system]
            [clojure.tools.logging :as log]
            [reitit.coercion.malli]
            [reitit.ring]
            [reitit.ring.middleware.parameters :as rrm-params]
            [ring.middleware.keyword-params :as rmk-params]
            [ring.util.response :as res])
  (:gen-class))

(defn ring-routes
  []
  (reitit.ring/routes
   (reitit.ring/redirect-trailing-slash-handler)
   ;; static assets nested under "resources/public"
   (reitit.ring/create-resource-handler {:path "/"})
   (reitit.ring/create-default-handler
    {:not-found (constantly (res/not-found "Not found."))})))

(def wrap-system
  {:name ::system
   :compile (fn [{:keys [system]} _]
              (fn [handler]
                (fn [req]
                  (handler (assoc req :system.application/system system)))))})

(def wrap-view-ctx
  {:name ::view-context
   :compile (fn [{ctx :system.application/view} _]
              (fn [handler]
                (fn [req]
                  (-> req
                      (assoc :system.application/view ctx)
                      handler))))})

(defmulti router
  :env)

(defmethod router :default
  [system]
  ;; Assume dev environment if no known env specified.
  (router (assoc system :env :dev)))

(defmethod router :dev
  [reitit-route-tree system]
  (reitit.ring/router
   reitit-route-tree
   {:data {:system system
           :coercions reitit.coercion.malli/coercion
           :middleware [wrap-view-ctx
                        rrm-params/parameters-middleware
                        rmk-params/wrap-keyword-params
                        wrap-system
                        #_[cors/wrap-cors :access-control-allow-origin #".*"
                           :access-control-allow-methods [:get :put :post :patch :delete]]]}}))

(defmulti app
  "Given a well-formed system definition, create a Ring app for the system's :runtime/env.

  TODO: Consider using Integrant's Profiles to drive this."
  :env)

(defmethod app :default
  [system]
  (reitit.ring/ring-handler
   (router system)
   (ring-routes)))

(defmethod app :dev
  [system]
  (reitit.ring/reloading-ring-handler
   #(reitit.ring/ring-handler
     (router system)
     (ring-routes))))

(defmethod system/build-config-map :system.application
  [{{:keys [middleware handler server]} :system.application/ring}]
  {::ring {:middleware (or (resolve (symbol middleware))
                           (fn [_handler]
                             (fn [request]
                               (log/error "Middleware absent. Please inject via settings.")
                               request)))
           :handler (or (resolve (symbol handler))
                        (fn [_request]
                          (log/error "Default handler. Please inject via settings.")
                          {:status 404
                           :body "Dummy handler. Please override."}))
           :server (merge {:type :jetty
                           :port 1337}
                          server)}})

(comment
  (system/build-config-map {:system.core/module :system.application
                            ::middleware 'identity
                            ::handler 'identity})
  )

(defmethod ig/init-key ::ring
  [_ middleware-stack]
  middleware-stack)

(defmethod ig/init-key ::handler
  [_ handler]
  handler)

(defmethod ig/init-key ::server
  [_ server-config]
  (log/info server-config)
  server-config)

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
                                      (select-keys data [:system.application/view]))))))})

    (def ring-router
      (reitit.ring/router
       ["" {}
        ["/api/ping" ::ping]
        ["/api/orders" {:middleware [wrap-application-view]
                        :get (fn [request]
                               {:status 200
                                :body (:system.application/view request "Not Found.")})
                        :system.application/view "bar"
                        :name ::foo}]]))

    (def app-try
      (reitit.ring/ring-handler
       ring-router)))

  (reitit.core/match-by-path ring-router
                             "/api/orders")

  (app-try {:request-method :get :uri "/api/orders"})

  (-> app-try (reitit.ring/get-router) (reitit.core/match-by-name ::foo))

  )
