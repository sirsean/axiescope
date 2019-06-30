(ns axiescope.views.panels.teams
  (:require
    [re-frame.core :as rf]
    [reagent-data-table.core :as rdt]
    [cuerdas.core :refer [format]]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [axie-table-headers axie-row-render-fn]]
    ))

(defn panel
  []
  (let [loading? @(rf/subscribe [:teams/loading?])
        teams @(rf/subscribe [:teams/teams])]
    [:div.container
     [header {:title "Teams"
              :bars [:teams]}]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading teams..."]]]
       [:div.row
        [:div.col-xs-12
         (doall
           (for [t teams]
             [:div.row {:key (:team-id t)}
              [:div.col-xs-12
               [:div.row {:style {:background-color "#DDDDDD"
                                  :padding "0.4em 0"}}
                [:div.col-xs-4
                 [:a {:href (format "https://axieinfinity.com/team/%s" (:team-id t))
                      :target "_blank"}
                  [:strong (:name t)]]]
                [:div.col-xs-4.center-xs]
                [:div.col-xs-4.end-xs
                 (if (:ready? t)
                   [:em "ready"]
                   [:span (format "ready in %sm" (:ready-in t))])]]
               [:div.row
                [:div.col-xs-12
                 (let [axies @(rf/subscribe [:teams/team-axies (:team-id t)])]
                   [rdt/data-table
                    {:headers axie-table-headers
                     :rows axies
                     :td-render-fn axie-row-render-fn}])]]]]))]
        ])
     [footer]]))
