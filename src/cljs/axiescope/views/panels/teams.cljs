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
     (if (and loading?
              (<= (count teams) 20))
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading teams..."]]]
       [:div.row
        (doall
          (for [t teams]
            [:div.col-xs-4 {:key (:team-id t)
                            :style {:padding "20px"}}
             [:div.row
              [:div.col-xs-12
               [:a {:href (format "https://axieinfinity.com/team/%s" (:team-id t))
                    :target "_blank"}
                [:strong (:name t)]]]]
             [:div.row {:style {:font-size "0.8em"}}
              [:div.col-xs-6]
              [:div.col-xs-2.end-xs "Tank"]
              [:div.col-xs-2.end-xs "DPS"]
              [:div.col-xs-2.end-xs "Support"]]
             (for [{:keys [id name image
                           tank-body dps-body support-body]}
                   @(rf/subscribe [:teams/team-axies (:team-id t)])
                   :when (some? id)]
               [:div.row.middle-xs {:key id}
                [:div.col-xs-3
                 [:img {:src image
                        :style {:width "100%"}}]]
                [:div.col-xs-3
                 [:a {:href (format "/axie/%s" id)}
                  name]]
                [:div.col-xs-2.end-xs
                 {:style (if (<= 4 tank-body)
                           {:color "green"
                            :font-weight "bold"}
                           {:color "#444444"
                            :font-size "0.9em"})}
                 tank-body]
                [:div.col-xs-2.end-xs
                 {:style (if (<= 4 dps-body)
                           {:color "green"
                            :font-weight "bold"}
                           {:color "#444444"
                            :font-size "0.9em"})}
                 dps-body]
                [:div.col-xs-2.end-xs
                 {:style (if (<= 3 support-body)
                           {:color "green"
                            :font-weight "bold"}
                           {:color "#444444"
                            :font-size "0.9em"})}
                 support-body]])]))])
     [footer]]))
