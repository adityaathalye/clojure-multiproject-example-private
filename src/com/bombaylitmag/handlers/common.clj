(ns com.bombaylitmag.handlers.common
  (:require [com.bombaylitmag.db.migrations :as db-migrate]
            [com.bombaylitmag.layouts.default :as default-layout]
            [hiccup.page :as hp]
            [hiccup2.core :as hc]
            [ring.util.response :as res]))

#_(def hydrate-view nil)

(defmulti hydrate-view
  "Handle requests based on `:mothra/view` context information injected
into the request map, by middleware. This way, we can declare context
information at the routing table, and conveniently trace route - view
relationship."
  (comp :template :mothra/view))

(defmethod hydrate-view :default
  [{{:keys [page-name content]} :mothra/view
    :as request}]
  (default-layout/page-template
   {:page-name (or page-name (:uri request))
    :content (or content [:p [:strong "Sorry, no content available."]])}))

(defmethod hydrate-view :echo
  [request]
  (list
   [:p "URI: " (:uri request)]
   [:p "Message: " "I'm still alive."]))

(defmethod hydrate-view :foo
  [request]
  (list
   [:p "URI: " (:uri request)]
   [:p "Message: " "I'm a FOO echoer"]))

(defn html-fragment
  "Return a fragment of HTML as 200 OK response. Presumably, the
  result of an HTMX API request."
  [req]
  (-> req
      (hydrate-view)
      (hc/html)
      (str)
      (res/response)
      (res/content-type "text/html")))

(defn html5-page
  "Return a complete HTML page as 200 OK response. Presumably, the
  result of navigating to a new page."
  [req]
  (-> req
      (hydrate-view)
      (hp/html5)
      (str)
      (res/response)
      (res/content-type "text/html")))

(comment
  (html5-page {:request-method :get :uri "/"})
  )
