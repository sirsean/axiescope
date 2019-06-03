(ns axiescope.views.panels.breedable
  (:require
    [re-frame.core :as rf]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [my-axies-table]]
    ))

(defn panel
  []
  (let [loading? @(rf/subscribe [:my-axies/loading?])]
    [:div.container
     [header {:title "Breedable"
              :bars [:my-axies]}]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       [my-axies-table {:sub :my-axies/breedable}])
     [footer]]))
