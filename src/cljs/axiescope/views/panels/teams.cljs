(ns axiescope.views.panels.teams
  (:require
    [re-frame.core :as rf]
    [reagent-table.core :as rt]
    [cuerdas.core :refer [format]]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [round axie-table-column-model axie-table-render-cell]]
    ))

(defn panel
  []
  (let [loading? @(rf/subscribe [:teams/loading?])
        teams @(rf/subscribe [:teams/teams])]
    [:div.container
     [header "Teams" [:teams]]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading teams..."]]]
       [:div.row
        [:div.col-xs-12
         (for [t teams]
           [:div.row {:key (:team-id t)}
            [:div.col-xs-12
             [:div.row {:style {:background-color "#DDDDDD"
                                :padding "0.4em 0"}}
              [:div.col-xs-4
               [:a {:href (format "https://axieinfinity.com/team/%s" (:team-id t))
                    :target "_blank"}
                [:strong (:name t)]]]
              [:div.col-xs-4.center-xs
               (when-some [record (:record t)]
                 (let [{:keys [wins losses wins-24 losses-24]} record
                       win-percentage (round (* 100 (/ wins (+ wins losses))) 2)]
                 [:span
                  (format "%s-%s [%s%] last 24: %s-%s"
                          wins losses win-percentage wins-24 losses-24)]))]
              [:div.col-xs-4.end-xs
               (if (:ready? t)
                 [:em "ready"]
                 [:span (format "ready in %sm" (:ready-in t))])]]
             [:div.row
              [:div.col-xs-12
               (let [axies (rf/subscribe [:teams/team-axies (:team-id t)])]
                 [rt/reagent-table
                  axies
                  {:column-model axie-table-column-model
                   :render-cell axie-table-render-cell}])]]]])]
        ])
     [footer]]))
