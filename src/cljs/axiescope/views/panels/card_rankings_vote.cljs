(ns axiescope.views.panels.card-rankings-vote
  (:require
    [re-frame.core :as rf]
    [axiescope.views.card :as card]
    [axiescope.views.layout :refer [header footer]]
    ))

(defn show-card
  [{:keys [id] :as card}
   other-id]
  [:div {:on-click (fn []
                     (rf/dispatch [:card-rankings/vote id other-id]))}
   [card/show card]])

(defn panel
  []
  [:div.container
   [header {:title "Vote"}]
   [:div.row
    [:div.col-xs-12
     [:p
      [:a {:href "/card-rankings"}
       "Back to the rankings"]]]]
   [:div.row
    [:div.col-xs-12.center-xs
     [:p "Click the card you think is better!"]]]
   (let [[left right] @(rf/subscribe [:card-rankings/pair])]
     [:div.row
      [:div.col-md-6.col-xs-12.center-xs
       [show-card left (:id right)]]
      [:div.col-md-6.col-xs-12.center-xs
       [show-card right (:id left)]]])
   [footer]])
