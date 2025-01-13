(ns usermanager.handlers.user-test
  (:require [clj-http.client :as http]
            [clojure.test :as t :refer [deftest is testing]]
            [usermanager.system.core :as system]
            [usermanager.http.middleware :as middleware]
            [usermanager.test-utilities :as tu]
            [usermanager.main :as um]
            [usermanager.router.core :as router]))

(def server (atom nil))

(def grug-system (atom nil))

(defn wrap-grug-test-middleware
  [handler system-components]
  (-> handler
      ;; keyword-params-middleware/wrap-keyword-params
      ;; params-middleware/wrap-params
      (middleware/wrap-grug-db system-components)
      middleware/wrap-message-param-in-response-header
      middleware/wrap-echo))

(defn ->test-grug-app-handler
  [system-components]
  (um/->grug-app-handler system-components
                         router/router
                         wrap-grug-test-middleware))

(defn with-server-object
  [->server-obj f]
  (reset! server (->server-obj))
  (f)
  (reset! server nil))

(def fixtures-legacy-system
  (t/join-fixtures
   [(partial tu/with-test-db ::db)
    (partial tu/setup-teardown-server!
             {:server-key ::server
              :middleware-key ::middleware
              :middleware-stack [#(middleware/wrap-db % ::db)
                                 middleware/wrap-message-param-in-response-header
                                 middleware/wrap-echo]})
    (partial with-server-object #(system/get-state ::server))]))

(def fixtures-grug-system
  (t/join-fixtures
   [(partial tu/setup-teardown-grug!
             {:com.adityaathalye.grugstack.system.server-simple/server
              {:handler-thunk `->test-grug-app-handler}}
             grug-system)
    (partial with-server-object #(get-in @grug-system
                                         [:com.adityaathalye.grugstack.system.server-simple/server
                                          :object]))]))

(t/use-fixtures :each
  (fn [f]
    (fixtures-legacy-system f)
    (reset! grug-system nil)
    (fixtures-grug-system f)))

#_(ns-unmap *ns* 'default-route-message-test)
(deftest default-route-message-test
  (testing "Testing that the default route injects a message in params."
    (let [base-uri (.toString (.getURI @server))]
      (is (= {:status 200
              :headers {"Content-Type" "text/plain;charset=utf-8"
                        "UM-Message"
                        (str "Welcome to the User Manager application demo!"
                             " "
                             "This is a first principles version of searncorfield/usermanager-example.")}
              :body "echoing METHOD :get for PATH /"}
             (-> (http/get base-uri)
                 (select-keys [:status :body :headers])
                 (update :headers (fn [{:strs [Content-Type UM-Message]}]
                                    {"Content-Type" Content-Type
                                     "UM-Message" UM-Message}))))))))

#_(ns-unmap *ns* 'reset-route-message-test)
(deftest reset-route-message-test
  (testing "Testing that the reset route injects a message in params."
    (let [base-uri (.toString (.getURI @server))]
      (is (= {:status 200
              :headers {"Content-Type" "text/plain;charset=utf-8"
                        "UM-Message" "The change tracker has been reset to 0."}
              :body "echoing METHOD :get for PATH /reset"}
             (-> (http/get (str base-uri "/reset"))
                 (select-keys [:status :body :headers])
                 (update :headers (fn [{:strs [Content-Type UM-Message]}]
                                    {"Content-Type" Content-Type
                                     "UM-Message" UM-Message}))))))))

#_(ns-unmap *ns* 'delete-route-params-wrapped-test)
(deftest delete-route-params-wrapped-test
  (testing "Testing that the delete route properly injects :id in :params."
    (let [base-uri (.toString (.getURI @server))]
      (is (= {:status 200
              :trace-redirects [(str base-uri "user/list")]
              :body "echoing METHOD :get for PATH /user/list"}
             (-> (http/get (str base-uri "user/delete/42"))
                 (select-keys [:status :body
                               :trace-redirects])))))))
