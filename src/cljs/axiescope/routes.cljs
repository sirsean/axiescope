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

  (defroute "/axie" []
    (rf/dispatch [::events/set-active-panel :axie-panel]))

  (defroute "/axie/:axie-id" {:keys [axie-id]}
    (rf/dispatch [::events/set-active-panel :axie-panel axie-id]))

  (defroute "/my-axies" []
    (rf/dispatch [::events/set-active-panel :my-axies-panel]))

  (defroute "/gallery" []
    (rf/dispatch [::events/set-active-panel :gallery-panel]))

  (defroute "/breedable" []
    (rf/dispatch [::events/set-active-panel :breedable-panel]))

  (defroute "/teams" []
    (rf/dispatch [::events/set-active-panel :teams-panel]))

  (defroute "/unassigned" []
    (rf/dispatch [::events/set-active-panel :unassigned-panel]))

  (defroute "/multi-assigned" []
    (rf/dispatch [::events/set-active-panel :multi-assigned-panel]))

  (defroute "/morph-to-petite" []
    (rf/dispatch [::events/set-active-panel :morph-to-petite-panel]))

  (defroute "/morph-to-adult" []
    (rf/dispatch [::events/set-active-panel :morph-to-adult-panel]))

  (defroute "/multi-gifter" []
    (rf/dispatch [::events/set-active-panel :multi-gifter-panel]))

  (defroute "/search" []
    (rf/dispatch [::events/set-active-panel :search-panel]))

  (defroute "/auto-battle" []
    (rf/dispatch [::events/set-active-panel :auto-battle-panel]))

  (accountant/configure-navigation!
    {:nav-handler secretary/dispatch!
     :path-exists? secretary/locate-route
     :reload-same-path? false})
  (accountant/dispatch-current!))
