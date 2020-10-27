(ns axiescope.views.panels.combo-rankings-add
  (:require
    [re-frame.core :as rf]
    [cuerdas.core :refer [format]]
    [axiescope.views.card :as card]
    [axiescope.views.shared :refer [sorter]]
    [axiescope.views.layout :refer [header footer]]
    ))

(defn show-card
  [{:keys [id] :as card}]
  [:div {:on-click (fn []
                     (rf/dispatch [:combo-rankings/add-select card]))}
   [card/show card]])

(defn mini-card
  [card]
  [:div {:style {:transform "scale(0.5, 0.5)"
                 :margin-top "-100px"}
         :on-click (fn []
                     (rf/dispatch [:combo-rankings/add-deselect card]))}
   [card/show card]])

(defn add-selection-box
  []
  (let [{:keys [mouth horn back tail] :as selections}
        @(rf/subscribe [:combo-rankings/add-selections])]
    (when (pos? (count selections))
      [:div.row {:style {:height "200px"
                         :margin-bottom "20px"}}
       [:div.col-xs-3
        (when (some? mouth)
          [mini-card mouth])]
       [:div.col-xs-3
        (when (some? horn)
          [mini-card horn])]
       [:div.col-xs-3
        (when (some? back)
          [mini-card back])]
       [:div.col-xs-3
        (when (some? tail)
          [mini-card tail])]])))

(defn add-button-box
  []
  (let [selections @(rf/subscribe [:combo-rankings/add-selections])
        loading? @(rf/subscribe [:combo-rankings/add-loading?])]
    (when (< 1 (count selections))
      [:div.row
       [:div.col-xs-12.center-xs
        [:button
         {:style {:padding "4px 8px"
                  :margin "0 0.1em"
                  :background-color "#2277bb"
                  :color "white"
                  :border "none"
                  :outline "none"
                  :border-radius "1em"}
          :disabled loading?
          :on-click #(rf/dispatch [:combo-rankings/add-submit])}
         "Add Combo"]]])))

(defn panel
  []
  (let [[left right] @(rf/subscribe [:combo-rankings/pair])]
    [:div.container
     [header {:title "Add Combo"}]
     [:div.row
      [:div.col-xs-12
       [:p
        [:a {:href "/combo-rankings"}
         "Back to the rankings"]]]]
     [add-selection-box]
     [add-button-box]
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
             [show-card card]])]))
     [footer]]))
