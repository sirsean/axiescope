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

  (defroute "/cards" []
    (rf/dispatch [::events/set-active-panel :cards-panel]))

  (defroute "/card-rankings/vote/:ranking-type" [ranking-type]
    (rf/dispatch [::events/set-active-panel
                  :card-rankings-vote-panel
                  (keyword ranking-type)]))

  (defroute "/card-rankings/:ranking-type" [ranking-type]
    (rf/dispatch [::events/set-active-panel
                  :card-rankings-panel
                  (keyword ranking-type)]))

  (defroute "/combo-rankings/vote" []
    (rf/dispatch [::events/set-active-panel :combo-rankings-vote-panel]))

  (defroute "/combo-rankings/add" []
    (rf/dispatch [::events/set-active-panel :combo-rankings-add-panel]))

  (defroute "/combo-rankings" []
    (rf/dispatch [::events/set-active-panel :combo-rankings-panel]))

  (defroute "/axie" []
    (rf/dispatch [::events/set-active-panel :axie-panel]))

  (defroute "/axie/:axie-id" {:keys [axie-id]}
    (rf/dispatch [::events/set-active-panel :axie-panel axie-id]))

  (defroute "/my-axies" []
    (rf/dispatch [::events/set-active-panel :my-axies-panel]))

  (defroute "/breedable" []
    (rf/dispatch [::events/set-active-panel :breedable-panel]))

  (defroute "/breed/calc/:sire-id/:matron-id" {:keys [sire-id matron-id]}
    (rf/dispatch [::events/set-active-panel :breed-calc-panel sire-id matron-id]))

  (defroute "/morph-to-petite" []
    (rf/dispatch [::events/set-active-panel :morph-to-petite-panel]))

  (defroute "/morph-to-adult" []
    (rf/dispatch [::events/set-active-panel :morph-to-adult-panel]))

  (defroute "/multi-gifter" []
    (rf/dispatch [::events/set-active-panel :multi-gifter-panel]))

  (accountant/configure-navigation!
    {:nav-handler secretary/dispatch!
     :path-exists? secretary/locate-route
     :reload-same-path? false})
  (accountant/dispatch-current!))
