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
