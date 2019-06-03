(ns axiescope.views.panels.morph-to-petite
  (:require
    [re-frame.core :as rf]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [my-axies-table]]
    ))

(defn panel
  []
  (let [loading? @(rf/subscribe [:my-axies/loading?])]
    [:div.container
     [header {:title "Morph to Petite"
              :bars [:my-axies]}]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       (if (empty? @(rf/subscribe [:my-axies/larva]))
         [:div.row
          [:div.col-xs-12.center-xs
           [:em "you have no axies that are ready to morph to petite"]]]
         [:div.row
          [:div.col-xs-12
           [:div.row
            [:div.col-xs-12
             [my-axies-table {:sub :my-axies/larva}]]]
           [:div.row
            [:div.col-xs-12.center-xs
             [:p
              [:a {:href "https://dappsuniverse.com/axie/mass-morph"
                   :target "_blank"}
               "go Mass Morph them"]]]]]]))
     [footer]]))
