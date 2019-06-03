(ns axiescope.views.panels.unassigned
  (:require
    [re-frame.core :as rf]
    [reagent-table.core :as rt]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [axie-sorter axie-table-column-model axie-table-render-cell]]
    ))

(defn panel
  []
  (let [axies-loading? @(rf/subscribe [:my-axies/loading?])
        teams-loading? @(rf/subscribe [:teams/loading?])]
    [:div.container
     [header {:title "Unassigned Axies"
              :bars [:my-axies :teams]}]
     (if (or axies-loading? teams-loading?)
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       (let [axies (rf/subscribe [:teams/unassigned-axies])]
         (if (empty? @axies)
           [:div.row
            [:div.col-xs-12.center-xs
             [:em "you have no unassigned axies"]]]
           [:div.row
            [:div.col-xs-12
             [axie-sorter {:section :my-axies}]]
            [:div.col-xs-12
             [rt/reagent-table
              axies
              {:table {:class "table table-striped"
                       :style {:margin "0 auto"}}
               :column-model axie-table-column-model
               :render-cell axie-table-render-cell}]]])))
     [footer]]))
