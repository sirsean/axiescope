(ns axiescope.views.panels.card-rankings-vote
  (:require
    [re-frame.core :as rf]
    [cuerdas.core :refer [format]]
    [axiescope.views.card :as card]
    [axiescope.views.layout :refer [header footer]]
    ))

(defn show-card
  [ranking-type
   {:keys [id] :as card}
   other-id]
  [:div {:on-click (fn []
                     (rf/dispatch [:card-rankings/vote ranking-type id other-id]))}
   [card/show card]])

(defn panel
  []
  (let [ranking-type @(rf/subscribe [:card-rankings/ranking-type])
        [left right] @(rf/subscribe [:card-rankings/pair])]
    [:div.container
     [header {:title "Vote"}]
     [:div.row
      [:div.col-xs-12
       [:p
        [:a {:href (format "/card-rankings/%s" (name ranking-type))}
         "Back to the rankings"]]]]
     [:div.row
      [:div.col-xs-12.center-xs
       (case ranking-type
         :all
         [:p "Click the card you think is better!"]

         :attack
         [:p "Click the card you think is better for attackers!"]

         :defense
         [:p "Click the card you think is better for defenders!"])]]
     [:div.row
      [:div.col-md-6.col-xs-12.center-xs
       [show-card ranking-type left (:id right)]]
      [:div.col-md-6.col-xs-12.center-xs
       [show-card ranking-type right (:id left)]]]
     [footer]]))
