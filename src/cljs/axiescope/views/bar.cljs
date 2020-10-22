(ns axiescope.views.bar
  (:require
   [re-frame.core :as rf]
   [cuerdas.core :refer [format]]
    ))

(defn loading-bar
  [numer denom color]
  (let [percent (max 10 (if (and numer denom (not= "?" denom))
                          (* 100 (/ numer denom))
                          0))]
      [:div {:style {:background-color color
                     :color "white"
                     :border-radius "0.15em"
                     :padding "0.2em"
                     :width (format "%s%" percent)
                     :height "100%"}}
       [:span (format "%s/%s" numer denom)]]))

(defn my-axies-bar
  []
  (let [loading? @(rf/subscribe [:my-axies/loading?])
        num-axies @(rf/subscribe [:my-axies/count])
        total-axies @(rf/subscribe [:my-axies/total])]
    [:div.row.middle-xs {:style {:margin-bottom "0.1em"}}
     [:div.col-xs-1.end-xs
      [:span "Axies"]]
     [:div.col-xs-10
      [loading-bar num-axies total-axies "#00b8ce"]]
     [:div.col-xs-1.end-xs
      [:button
       {:disabled loading?
        :on-click #(rf/dispatch [:my-axies/fetch true])}
       "Reload"]]]))

(defn teams-bar
  []
  (let [loading? @(rf/subscribe [:teams/loading?])
        num-teams @(rf/subscribe [:teams/count])
        total-teams @(rf/subscribe [:teams/total])]
    [:div.row.middle-xs {:style {:margin-bottom "0.1em"}}
     [:div.col-xs-1.end-xs
      [:span "Teams"]]
     [:div.col-xs-10
      [loading-bar num-teams total-teams "#6cc000"]]
     [:div.col-xs-1.end-xs
      [:button
       {:disabled loading?
        :on-click #(rf/dispatch [:teams/fetch-teams true])}
       "Reload"]]]))

(defn items-bar
  []
  (let [loading? @(rf/subscribe [:land/items-loading?])
        num-items @(rf/subscribe [:land/items-count])
        total-items @(rf/subscribe [:land/total-items])]
    [:div.row.middle-xs {:style {:margin-bottom "0.1em"}}
     [:div.col-xs-1.end-xs
      [:span "My Items"]]
     [:div.col-xs-10
      [loading-bar num-items total-items "#c88ae0"]]
     [:div.col-xs-1.end-xs
      [:button
       {:disabled loading?
        :on-click #(rf/dispatch [:land/fetch-items true])}
       "Reload"]]]))

(defn market-bar
  []
  (let [loading? @(rf/subscribe [:land/market-loading?])
        num-items @(rf/subscribe [:land/market-count])
        total-items @(rf/subscribe [:land-market/total])]
    [:div.row.middle-xs {:style {:margin-bottom "0.1em"}}
     [:div.col-xs-1.end-xs
      [:span "Market"]]
     [:div.col-xs-10
      [loading-bar num-items total-items "#c88ae0"]]
     [:div.col-xs-1.end-xs
      [:button
       {:disabled loading?
        :on-click #(rf/dispatch [:land/fetch-market true])}
       "Reload"]]]))
