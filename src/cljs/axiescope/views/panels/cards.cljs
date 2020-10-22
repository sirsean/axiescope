(ns axiescope.views.panels.cards
  (:require
    [re-frame.core :as rf]
    [accountant.core :as accountant]
    [axiescope.views.card :as card]
    [axiescope.views.shared :refer [sorter]]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.util :refer [round]]
    ))

(defn search-box
  []
  [:div.row
   [:div.col-xs-12.center-xs
    [:input {:style {:margin "2em"
                     :padding "0.5em"}
             :type "text"
             :placeholder "Search..."
             :on-change (fn [e]
                          (rf/dispatch [:cards/set-search (-> e .-target .-value)]))}]]])

(defn selector-button
  [section selector active-selector]
  (let [active? (= selector active-selector)]
    [:button
     {:style {:padding "4px 8px"
              :margin "0 0.1em"
              :background-color (if active?
                                  "#2277bb"
                                  "#bcd6ea")
              :color (if active?
                       "white"
                       "black")
              :border "none"
              :outline "none"
              :border-radius "1em"}
      :disabled active?
      :on-click #(rf/dispatch [(keyword section :set-selector) selector])}
     (name selector)]))

(defn selectors
  [section selectors]
  (let [active-selector @(rf/subscribe [(keyword section :selector)])]
  [:div.row
   [:div.col-xs-12.center-xs
    {:style {:margin "0.4em"}}
    (for [selector selectors]
      ^{:key selector}
      [selector-button section selector active-selector])]]))

(defn panel
  []
  [:div.container
   [header {:title "Cards"}]
   [search-box]
   [selectors :cards [:all
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
