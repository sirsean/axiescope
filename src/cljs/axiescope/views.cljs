(ns axiescope.views
  (:require
    [re-frame.core :as rf]
    [accountant.core :as accountant]
    [reagent.core :as r]
    [reagent-table.core :as rt]
    [clojure.string :as string]
    [cuerdas.core :refer [format]]
    [axiescope.subs :as subs]
    [axiescope.events :as events]
    [axiescope.moves :as moves]
    [axiescope.views.panels.axie :as axie]
    [axiescope.views.panels.my-axies :as my-axies]
    [axiescope.views.panels.breedable :as breedable]
    [axiescope.views.panels.breed-calc :as breed-calc]
    [axiescope.views.panels.morph-to-petite :as morph-to-petite]
    [axiescope.views.panels.morph-to-adult :as morph-to-adult]
    [axiescope.views.panels.multi-gifter :as multi-gifter]
    [axiescope.views.panels.land :as land]
    [axiescope.views.panels.card-rankings :as card-rankings]
    [axiescope.views.panels.card-rankings-vote :as card-rankings-vote]
    [axiescope.views.panels.cards :as cards]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [axies-pager axie-sorter sort-key-button sort-order-button my-axies-table]]
    ))

(defn home-panel []
  [:div.container
   [:div.row
    [:div.col-xs-12
     [:h1 "axiescope"]]]
   [:div.row
    [:div.col-xs-12
     [:p "These are a collection of tools to help you with Axie Infinity."]]]
   [:div.row
    [:div.col-xs-12
     [:ul.tools-list
      [:li [:a {:href "/cards"}
            "Cards"]]
      [:li [:a {:href "/card-rankings"}
            "Card Rankings"]]
      [:li [:a {:href "/axie"}
            "Axie Evaluator"]]
      [:li [:a {:href "/my-axies"}
            "My Axies"]]
      [:li [:a {:href "/breedable"}
            "Breedable"]]
      [:li [:a {:href "/morph-to-petite"}
            "Morph to Petite"]]
      [:li [:a {:href "/morph-to-adult"}
            "Morph to Adult"]]
      [:li [:a {:href "/multi-gifter"}
            "Multi-Gifter"]]
      #_[:li [:a {:href "/land"}
            "Land"]]]]]
   [footer]])

(defn get-panel
  [panel]
  (case panel
    :home-panel [home-panel]
    :axie-panel [axie/panel]
    :my-axies-panel [my-axies/panel]
    :breedable-panel [breedable/panel]
    :breed-calc-panel [breed-calc/panel]
    :morph-to-petite-panel [morph-to-petite/panel]
    :morph-to-adult-panel [morph-to-adult/panel]
    :multi-gifter-panel [multi-gifter/panel]
    :land-panel [land/land-panel]
    :land-items-panel [land/land-items-panel]
    :land-market-panel [land/land-market-panel]
    :land-valuation-panel [land/land-valuation-panel]
    :card-rankings-panel [card-rankings/panel]
    :card-rankings-vote-panel [card-rankings-vote/panel]
    :cards-panel [cards/panel]
    [home-panel]))

(defn show-panel
  [panel]
  [get-panel panel])

(defn main-panel []
  (let [active-panel @(rf/subscribe [::subs/active-panel])]
    [show-panel active-panel]))
