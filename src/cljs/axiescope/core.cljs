(ns axiescope.core
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [re-graph.core :as re-graph]
   [axiescope.events :as events]
   [axiescope.views :as views]
   [axiescope.config :as config]
   [axiescope.routes :as routes]
   ))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch [::re-graph/init
                      {:ws {:url nil}
                       :http {:url "https://axieinfinity.com/graphql-server/graphql"
                              :impl {}
                              :supported-operations #{:query :mutate}}}])
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
