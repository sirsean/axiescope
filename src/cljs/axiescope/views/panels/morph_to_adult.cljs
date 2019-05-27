(ns axiescope.views.panels.morph-to-adult
  (:require
    [re-frame.core :as rf]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [my-axies-table]]
    ))

(defn panel
  []
  (let [loading? @(rf/subscribe [:my-axies/loading?])]
    [:div.container
     [header "Morph to Adult" [:my-axies]]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       (if (empty? @(rf/subscribe [:my-axies/petite]))
         [:div.row
          [:div.col-xs-12.center-xs
           [:em "you have no axies that are ready to morph to adult"]]]
         [:div.row
          [:div.col-xs-12
           [:div.row
            [:div.col-xs-12
             [my-axies-table {:sub :my-axies/petite}]]]
           [:div.row
            [:div.col-xs-12.center-xs
             [:p
              [:a {:href "https://dappsuniverse.com/axie/mass-morph"
                   :target "_blank"}
               "go Mass Morph them"]]]]]]))
     [footer]]))
