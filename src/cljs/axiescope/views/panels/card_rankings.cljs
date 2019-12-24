(ns axiescope.views.panels.card-rankings
  (:require
    [re-frame.core :as rf]
    [reagent-data-table.core :as rdt]
    [accountant.core :as accountant]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.util :refer [round]]
    ))

(def rankings-headers
  [[:rank "Rank"]
   [:part-name "Part"]
   [:skill-name "Skill"]
   [:trigger-text "Move"]
   [:rating "Rating"]])

(defn rankings-render-fn
  [row key]
  (let [value (get row key)]
    (case key
      :rating [:td (round value 0)]
      [:td value])))

(defn rankings-table
  [rankings]
  [rdt/data-table
   {:headers      rankings-headers
    :rows         rankings
    :td-render-fn rankings-render-fn}])

(defn panel
  []
  [:div.container
   [header {:title "Card Rankings"}]
   [:div.row
    [:div.col-xs-12
     [:div.row
      [:div.col-xs-12.center-xs
       [:p "Vote for what cards you think are better, to help the community come up with a shared ranking."]]
      [:div.col-xs-12.center-xs
       [:button {:on-click (fn [e]
                             (.preventDefault e)
                             (accountant/navigate! "/card-rankings/vote"))
                 :style {:padding "0.9em"
                         :margin "0.5em"
                         :font-size "1.1em"
                         :background-color "#2277bb"
                         :color "white"
                         :border "none"
                         :outline "none"
                         :border-radius "1.8em"}}
        "Help By Voting!"]]]
     (let [loading? @(rf/subscribe [:card-rankings/loading?])
           rankings @(rf/subscribe [:card-rankings/rankings])]
       (if loading?
         [:div.row
          [:div.col-xs-12.center-xs
           [:em "loading..."]]]
         [:div.row
          [:div.col-xs-12
           [rankings-table rankings]]]))]]
   [footer]])
