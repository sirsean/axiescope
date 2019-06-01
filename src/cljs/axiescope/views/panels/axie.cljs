(ns axiescope.views.panels.axie
  (:require
    [re-frame.core :as rf]
    [reagent.core :as r]
    [accountant.core :as accountant]
    [cuerdas.core :refer [format]]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [show-axie axie-info]]
    ))

(defn panel
  []
  (let [loading? @(rf/subscribe [:axie/loading?])
        axie-id @(rf/subscribe [:axie/axie-id])
        axie @(rf/subscribe [:axie/axie axie-id])
        axie-id-atom (r/atom axie-id)]
    [:div.container
     [header "Axie Evaluator"]
     [:div.row
      [:div.col-xs-12
       [:form {:on-submit (fn [e]
                            (.preventDefault e)
                            (accountant/navigate! (format "/axie/%s" @axie-id-atom)))}
        [:div.row.middle-xs
         [:div.col-xs-1.end-xs
          [:span "Axie ID"]]
         [:div.col-xs-10.center-xs
          [:input {:type "text"
                   :name "axie-id"
                   :on-change (fn [e]
                                (reset! axie-id-atom (-> e .-target .-value)))
                   :style {:width "100%"
                           :font-size "1.2em"
                           :padding "0.3em"}}]]
         [:div.col-xs-1.end-xs
          [:button {:style {:padding "0.5em"
                            :font-size "1.0em"}}
           "Evaluate"]]]]]]
     (if (and loading? (nil? axie))
       [:div.row
        [:div.col-xs-12.center-xs
         [:p [:em "loading..."]]]]
       [:div.row
        [:div.col-xs-12.col-md-6
         [show-axie axie]]
        [:div.col-xs-12.col-md-6
         [axie-info axie]]
        [:div.col-xs-12.center-xs
         [:a {:href (format "/lineage/%s" axie-id)}
          "Lineage"]]])
     [footer]]))
