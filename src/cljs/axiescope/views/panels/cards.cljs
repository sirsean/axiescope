(ns axiescope.views.panels.cards
  (:require
    [re-frame.core :as rf]
    [axiescope.views.card :as card]
    [axiescope.views.shared :refer [sorter]]
    [axiescope.views.layout :refer [header footer]]
    ))

(defn panel
  []
  [:div.container
   [header {:title "Cards"}]
   [card/search-box]
   [card/selectors :cards [:all
                           :horn
                           :mouth
                           :back
                           :tail
                           :plant
                           :bird
                           :bug
                           :beast
                           :aquatic
                           :reptile]]
   [sorter {:section :cards
            :fields [["id" :id]
                     ["attack" :default-attack]
                     ["defense" :default-defense]
                     ["energy" :default-energy]]}]
   (let [loading? @(rf/subscribe [:cards/loading?])
         cards @(rf/subscribe [:cards/list])]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       [:div.row
        (for [card cards]
          [:div.col-md-3.col-xs-12
           {:key (:id card)
            :style {:margin-bottom "0.4em"}}
           [card/show card]])]))
   [footer]])
