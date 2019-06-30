(ns axiescope.views.panels.multi-assigned
  (:require
    [re-frame.core :as rf]
    [reagent-data-table.core :as rdt]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [axie-table-headers axie-row-render-fn]]
    ))

(defn panel
  []
  (let [loading? @(rf/subscribe [:teams/loading?])
        axies @(rf/subscribe [:teams/multi-assigned-axies])]
    [:div.container
     [header {:title "Multi-Assigned Axies"
              :bars [:my-axies :teams]}]
     [:div.row
      [:div.col-xs-12.center-xs
       [:p "(These axies are on more than one team right now.)"]]]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       (if (empty? axies)
         [:div.row
          [:div.col-xs-12.center-xs
           [:em "you have no axies assigned to multiple teams"]]]
         [:div.row
          [:div.col-xs-12
           [rdt/data-table
            {:rows axies
             :headers (conj axie-table-headers [:team-name "Team"])
             :td-render-fn axie-row-render-fn}]]]))
     [footer]]))
