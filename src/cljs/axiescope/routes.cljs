(ns axiescope.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:require
   [secretary.core :as secretary]
   [accountant.core :as accountant]
   [re-frame.core :as rf]
   [axiescope.events :as events]
   ))

(defn app-routes []
  (defroute "/" []
    (rf/dispatch [::events/set-active-panel :home-panel]))

  (defroute "/battle-simulator" []
    (rf/dispatch [::events/set-active-panel :battle-simulator-panel]))

  (defroute "/my-axies" []
    (rf/dispatch [::events/set-active-panel :my-axies-panel]))

  (defroute "/breedable" []
    (rf/dispatch [::events/set-active-panel :breedable-panel]))

  (defroute "/teams" []
    (rf/dispatch [::events/set-active-panel :teams-panel]))

  (accountant/configure-navigation!
    {:nav-handler secretary/dispatch!
     :path-exists? secretary/locate-route
     :reload-same-path? false})
  (accountant/dispatch-current!))
