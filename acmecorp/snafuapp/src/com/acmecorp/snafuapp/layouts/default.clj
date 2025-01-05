(ns com.acmecorp.snafuapp.layouts.default
  (:require [hiccup.page :as hp]))

(defn page-template
  "Structure of the default complete HTML5 page."
  [{:keys [page-name
           content]
    :or {page-name ""
         content [:p [:strong "Sorry, no content available."]]}}]
  (list
   [:head
    [:meta {:charset  "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:title (format "%s - %s" "SNAFU" page-name)]
    ;; static assets should be under 'resources/public'
    [:link {:rel "icon" :type "image/png" :href "/img/favicon.png"}]
    (hp/include-css "/css/style.css")]
   [:body
    [:div {:id "site-top" :class "stack center"}
     [:header {:id "site-header"}
      [:h2 "Snafuapp / " [:small [:small page-name]]]
      [:nav
       [:a {:href "/"
            :title "ACME Corp. - Planetary SNAFU Sniffer"}
        "Home"]]]
     [:main {:id "main"}
      content]
     [:footer {:id "site-footer" :class "stack"}
      [:hr]
      [:p "An ACME Corp. SNAFU"]]]]))

(comment
  (hp/html5 (page-template {}))
  )
