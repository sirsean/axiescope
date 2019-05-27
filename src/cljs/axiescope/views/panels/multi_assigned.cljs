(ns axiescope.views.panels.multi-assigned
  (:require
    [re-frame.core :as rf]
    [reagent-table.core :as rt]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [axie-table-column-model axie-table-render-cell]]
    ))

(defn panel
  []
  (let [loading? @(rf/subscribe [:teams/loading?])
        axies (rf/subscribe [:teams/multi-assigned-axies])]
    [:div.container
     [header "Multi-Assigned Axies" [:my-axies :teams]]
     [:div.row
      [:div.col-xs-12.center-xs
       [:p "(These axies are on more than one team right now.)"]]]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       (if (empty? @axies)
         [:div.row
          [:div.col-xs-12.center-xs
           [:em "you have no axies assigned to multiple teams"]]]
         [:div.row
          [:div.col-xs-12
           [rt/reagent-table
            axies
            {:table {:class "table table-striped"
                     :style {:margin "0 auto"}}
             :column-model (conj axie-table-column-model {:header "Team"
                                                          :key :team-name})
             :render-cell axie-table-render-cell}]]]))
     [footer]]))
