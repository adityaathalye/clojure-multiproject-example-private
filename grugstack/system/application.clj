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

(def reitit-route-tree-hello-world-app
  [""
   ["/" {:get (constantly {:status 200
                           :headers {"Content-Type" "text/html"}
                           :body "<h1>Hello World.</h1>"})}]
   ["/ping" {:get (constantly {:status 200 :body "Pong"})}]])

(defn reitit-ring-default-routes
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

(defmulti reitit-ring-router
  (fn [system _reitit-route-tree]
    (get-in system [:environment :type])))

(defmethod reitit-ring-router :default
  [system reitit-route-tree]
  ;; Assume dev environment if no known env specified.
  (reitit-ring-router (assoc-in system [:environment :type] :dev)
                      reitit-route-tree))

(defmethod reitit-ring-router :dev
  [system reitit-route-tree]
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

(defmulti reitit-ring-handler
  "Given a well-formed system definition, create a Ring app for the system's :runtime/env.

  TODO: Consider using Integrant's Profiles to drive this."
  :env)

(defmethod reitit-ring-handler :default
  [{:keys [system reitit-route-tree]}]
  (reitit.ring/ring-handler
   (reitit-ring-router system reitit-route-tree)
   (reitit-ring-default-routes)))

(defmethod reitit-ring-handler :dev
  [{:keys [system reitit-route-tree]}]
  (reitit.ring/reloading-ring-handler
   #(reitit.ring/ring-handler
     (reitit-ring-router system reitit-route-tree)
     (reitit-ring-default-routes))))

(defmethod system/build-config-map :system.application
  [{:system.application/keys [router handler reitit-route-tree]
    :or {router 'system.application/reitit-ring-router
         handler 'system.application/reitit-ring-handler
         reitit-route-tree 'system.application/reitit-route-tree-hello-world-app}}]
  {::reitit-ring {:data {:system (ig/ref :system.core/system)
                         :coercions reitit.coercion.malli/coercion
                         :middleware [wrap-view-ctx
                                      rrm-params/parameters-middleware
                                      rmk-params/wrap-keyword-params
                                      wrap-system
                                      #_[cors/wrap-cors :access-control-allow-origin #".*"
                                         :access-control-allow-methods [:get :put :post :patch :delete]]]}}
   ::reitit-route-tree reitit-route-tree
   ::reitit-router router
   ::handler handler})

(defmethod ig/init-key ::reitit-ring
  [_ router-parts]
  (log/info router-parts)
  router-parts)

(defmethod ig/init-key ::reitit-router
  [_ router]
  (resolve router))

(defmethod ig/init-key ::reitit-route-tree
  [_ reitit-route-tree]
  (deref (resolve reitit-route-tree)))

(defmethod ig/init-key ::handler
  [_ handler]
  (resolve handler))

(comment
  (ig/init
   (merge (system/build-config-map {:system.core/module :system.application})
          {:system.core/system {:environment {:type :dev}}}))
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
