(ns com.bombaylitmag.middleware.core)

(def wrap-system
  {:name ::system
   :compile (fn [{:keys [system]} _]
              (fn [handler]
                (fn [req]
                  (handler (assoc req :mothra/system system)))))})

(def wrap-view-ctx
  {:name ::view-context
   :compile (fn [{ctx :mothra/view} _]
              (fn [handler]
                (fn [req]
                  (-> req
                      (assoc :mothra/view ctx)
                      handler))))})
