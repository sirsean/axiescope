(ns axiescope.views.panels.multi-gifter
  (:require
    [re-frame.core :as rf]
    [reagent-table.core :as rt]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [axies-pager axie-sorter axie-table-column-model axie-table-render-cell]]
    ))

(defn panel
  []
  (let [followers-farm-addr "0x8EaDBb0209ca4D1c299Dc403dEAE40421e2e075B"
        loading? @(rf/subscribe [:my-axies/loading?])
        to-addr @(rf/subscribe [:multi-gifter/to-addr])]
    [:div.container
     [header "Multi-Gifter" [:my-axies]]
     [:div.row
      [:div.col-xs-12.center-xs
       [:p "You can send gifts faster this way."]]]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       [:div.row
        [:div.col-xs-12
         [:div.row
          [:div.col-xs-12.center-xs
           [:h2 "The first thing you have to do is set the recipient's address:"]
           [:p
            [:input {:id "multi-gifter-to-addr"
                     :type "text"
                     :style {:padding "0.4em"
                             :font-size "1.2em"
                             :text-align "center"
                             :width "60%"}
                     :default-value to-addr
                     :on-change (fn [e]
                                  (rf/dispatch [:multi-gifter/set-to-addr
                                                (-> e .-target .-value)]))}]]
           [:p "Donate to " [:a {:href "#"
                                 :on-click (fn [e]
                                             (-> js/document
                                                 (.getElementById "multi-gifter-to-addr")
                                                 (aset "value" followers-farm-addr))
                                             (rf/dispatch [:multi-gifter/set-to-addr
                                                           followers-farm-addr])
                                             (.preventDefault e))}
                             "Followers FARM"]
            " to help the community grow."]]]
         [:div.row
          [:div.col-xs-12.center-xs
           [:h2 "and then you can send any axies as gifts just by clicking the Gift button..."]
           (let [axies (rf/subscribe [:my-axies/axies])]
             [:div.row
              [:div.col-xs-12
               [axie-sorter {:section :my-axies}]]
              [:div.col-xs-12
               [rt/reagent-table
                axies
                {:table {:class "table table-striped"
                         :style {:margin "0 auto"}}
                 :column-model (conj axie-table-column-model {:header ""
                                                              :key :gift-button})
                 :render-cell axie-table-render-cell}]]])]]]])
     [footer]]))
