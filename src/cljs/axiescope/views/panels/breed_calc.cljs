(ns axiescope.views.panels.breed-calc
  (:require
    [re-frame.core :as rf]
    [cuerdas.core :refer [format]]
    [reagent-data-table.core :as rdt]
    [axiescope.views.shared :refer [axie-row-render-fn parts-row]]
    ))

(def table-headers
  [[:parts ""]
   [:tank-body "Tank"]
   [:dps-body "DPS"]
   [:support-body "Support"]])

(defn panel
  []
  (let [sire @(rf/subscribe [:breed-calc/sire])
        matron @(rf/subscribe [:breed-calc/matron])
        can-breed? @(rf/subscribe [:breed-calc/can-breed? sire matron])
        prediction @(rf/subscribe [:breed-calc/prediction sire matron])
        predicted-score @(rf/subscribe [:breed-calc/predict-score sire matron])]
    [:div.container
     [:h1 "Breed Calc"]
     [:div.row
      [:div.col-xs-6.center-xs
       [:h4 (:name sire)]
       [parts-row sire]]
      [:div.col-xs-6.center-xs
       [:h4 (:name matron)]
       [parts-row matron]]]
     (if-not can-breed?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "These axies are related and cannot breed."]]]
       [:div
        (when (and (some? predicted-score)
                   (not (js/isNaN (:tank-body predicted-score))))
          [:div
           [:div.row
            [:div.col-xs-6.end-xs
             [:strong "Tank"]]
            [:div.col-xs-6
             (:tank-body predicted-score)]]
           [:div.row
            [:div.col-xs-6.end-xs
             [:strong "DPS"]]
            [:div.col-xs-6
             (:dps-body predicted-score)]]
           [:div.row
            [:div.col-xs-6.end-xs
             [:strong "Support"]]
            [:div.col-xs-6
             (:support-body predicted-score)]]])
        (when (seq prediction)
          [:div.row
           [:div.col-xs-12
            [rdt/data-table
             {:headers table-headers
              :rows prediction
              :td-render-fn axie-row-render-fn}]]])])]))
