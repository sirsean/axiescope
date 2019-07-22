(ns axiescope.views.panels.breedable
  (:require
    [re-frame.core :as rf]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [axies-pager my-axies-table]]
    ))

(def breedable-headers
  [[:id "ID"]
   [:image ""]
   [:name "Name"]
   [:parts ""]
   [:breed-count "Breeds"]
   [:atk+def "Atk+Def"]
   [:tank-body "Tank"]
   [:dps-body "DPS"]
   [:support-body "Support"]
   [:sire-selector "Sire"]
   [:matron-selector "Matron"]])

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
       [:div.row
        [:div.col-xs-12
         [my-axies-table {:sub :my-axies/breedable
                          :headers breedable-headers}]]
        [:div.col-xs-12
         [axies-pager :breedable]]])
     [footer]]))
