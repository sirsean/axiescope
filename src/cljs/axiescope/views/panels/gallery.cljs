(ns axiescope.views.panels.gallery
  (:require
    [re-frame.core :as rf]
    [cuerdas.core :refer [format]]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [axie-sorter]]
    ))

(defn panel
  []
  (let [loading? @(rf/subscribe [:my-axies/loading?])
        num-axies @(rf/subscribe [:my-axies/count])
        axies @(rf/subscribe [:my-axies/axies])]
    [:div.container
     [header {:title "Axie Gallery"
              :bars [:my-axies]}]
     (if (and loading?
              (< num-axies 10))
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       [:div.row
        [:div.col-xs-12
         [axie-sorter {:section :my-axies}]]
        [:div.col-xs-12
         [:div.row.middle-xs
          (for [{:keys [id image]} axies]
            [:div.col-xs-2.center-xs {:key id}
             [:a {:href (format "/axie/%s" id)}
              [:img {:style {:width "100%"}
                     :src image}]]])]]])
     [footer]]))
