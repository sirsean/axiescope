(ns axiescope.views.panels.my-axies
  (:require
    [re-frame.core :as rf]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [my-axies-table axies-pager]]
    ))

(defn panel
  []
  (let [loading? @(rf/subscribe [:my-axies/loading?])
        num-axies @(rf/subscribe [:my-axies/count])
        page-size @(rf/subscribe [:my-axies/page-size])]
    [:div.container
     [header "My Axies" [:my-axies]]
     (if (and loading?
              (< num-axies page-size))
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       [:div.row
        [:div.col-xs-12
         [my-axies-table {:sub :my-axies/axies}]]
        [:div.col-xs-12
         [axies-pager :my-axies]]])
     [footer]]))
