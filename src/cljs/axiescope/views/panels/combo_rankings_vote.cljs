(ns axiescope.views.panels.combo-rankings-vote
  (:require
    [re-frame.core :as rf]
    [cuerdas.core :refer [format]]
    [axiescope.views.card :as card]
    [axiescope.views.layout :refer [header footer]]
    ))

(defn mini-card
  [card]
  [:div.col-xs-3 {:style {:transform "scale(0.5, 0.5)"}}
   [card/show card]])

(defn show-combo
  [{:keys [id] :as combo}
   other-id]
  [:div.row {:on-click (fn []
                         (rf/dispatch [:combo-rankings/vote id other-id]))}
   (for [card (:cards combo)]
     ^{:key (:id card)}
     [mini-card card])])

(defn panel
  []
  (let [[left right] @(rf/subscribe [:combo-rankings/pair])]
    [:div.container
     [header {:title "Vote"}]
     [:div.row
      [:div.col-xs-12
       [:p
        [:a {:href "/combo-rankings"}
         "Back to the rankings"]]]]
     [:div.row
      [:div.col-xs-12.center-xs
       [:p "Click the combo you think is better!"]]]
     [:div.row
      [:div.col-md-6.col-xs-12.center-xs
       [show-combo left (:id right)]]
      [:div.col-md-6.col-xs-12.center-xs
       [show-combo right (:id left)]]]
     [footer]]))
