(ns org.evalapply.smol-web-app-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as http]
            [org.evalapply.smol-web-app :as swa]))

(deftest a-simple-server-test
  (testing "A simple server test."
    (let [server (swa/-main)
          base-uri (.toString (.getURI server))]
      (is (= {:status 200
              :headers {"Content-Type" "text/plain;charset=utf-8"}
              :body "echoing METHOD :get for PATH /"}
             (-> (http/get base-uri)
                 (select-keys [:status :body :headers])
                 (update :headers (fn [{:strs [Content-Type]}]
                                    {"Content-Type" Content-Type}))))
          "Server echoes back request information in body.")
      (is (= {:status 404
              :body "Not Found."}
             (-> (http/post base-uri {:throw-exceptions false})
                 (select-keys [:status :body])))
          "Server rejects unsupported route pattern.")
      (.stop server))))
