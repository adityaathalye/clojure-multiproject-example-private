(ns usermanager.main-grug-test
  (:require
   [clj-http.client :as http]
   [clojure.test :as t :refer [deftest is testing]]
   [usermanager.test-utilities :as tu]))

(def grug-system (atom nil))

(t/use-fixtures :once (partial tu/setup-teardown-grug! grug-system))

(defn server-obj []
  (get-in @grug-system
          [:com.adityaathalye.grugstack.system.server-simple/server
           :object]))

(deftest a-simple-server-test
  (testing "A simple server test."
    (let [base-uri (.toString (.getURI (server-obj)))]
      (is (= {:status 200
              :headers {"Content-Type" "text/html"}}
             (-> (http/get base-uri)
                 (select-keys [:status :headers])
                 (update :headers (fn [{:strs [Content-Type]}]
                                    {"Content-Type" Content-Type}))))
          "Server echoes back information about request method and uri.")
      (is (= {:status 404
              :body "Not Found."}
             (-> (http/post base-uri {:throw-exceptions false})
                 (select-keys [:status :body])))
          "Server rejects unsupported route pattern."))))
